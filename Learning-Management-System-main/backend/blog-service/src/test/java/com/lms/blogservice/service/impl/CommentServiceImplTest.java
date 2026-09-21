package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.request.UpdateCommentRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.entity.Comment;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.CommentMapper;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    PostRepository postRepository;
    @Mock
    CommentRepository commentRepository;
    @Mock
    VoteRepository voteRepository;
    @Mock
    CommentMapper commentMapper;

    @InjectMocks
    CommentServiceImpl commentService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void getCommentsRejectsUnpublishedPost() {
        Post post = Post.builder().id(1L).status(PostStatus.DRAFT).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.getComments(1L, PageRequest.of(0, 10)))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_PUBLISHED);
    }

    @Test
    void getCommentsReturnsRootCommentsWithRepliesAndVoteCounts() {
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Comment root = Comment.builder().id(10L).post(post).userId("user-1").content("Root").build();
        Comment reply = Comment.builder().id(11L).post(post).parent(root).userId("user-2").content("Reply").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdAndParentIsNull(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(root)));
        when(commentRepository.findByParentId(10L)).thenReturn(List.of(reply));
        when(voteRepository.countByTargetIdsAndTypeAndVoteType(List.of(10L, 11L), TargetType.COMMENT, VoteType.UPVOTE))
                .thenReturn(List.of(new Object[]{10L, 2L}, new Object[]{11L, 1L}));
        when(voteRepository.countByTargetIdsAndTypeAndVoteType(List.of(10L, 11L), TargetType.COMMENT, VoteType.DOWNVOTE))
                .thenReturn(List.of());
        when(commentMapper.toCommentResponse(root)).thenReturn(CommentResponse.builder().id(10L).content("Root").build());
        when(commentMapper.toCommentResponse(reply)).thenReturn(CommentResponse.builder().id(11L).content("Reply").build());

        var page = commentService.getComments(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getUpvoteCount()).isEqualTo(2);
            assertThat(response.getReplies()).singleElement()
                    .extracting(CommentResponse::getUpvoteCount)
                    .isEqualTo(1L);
        });
    }

    @Test
    void createCommentCreatesRootCommentForPublishedPost() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(10L);
            return comment;
        });
        when(commentMapper.toCommentResponse(any(Comment.class))).thenReturn(CommentResponse.builder().id(10L).content("Nice").build());

        CommentResponse response = commentService.createComment(
                1L,
                CreateCommentRequest.builder().content("Nice").build());

        assertThat(response.getId()).isEqualTo(10L);
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getPost()).isSameAs(post);
        assertThat(captor.getValue().getUserId()).isEqualTo("student-1");
        assertThat(captor.getValue().getParent()).isNull();
    }

    @Test
    void createReplyRejectsParentFromAnotherPost() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Post otherPost = Post.builder().id(2L).status(PostStatus.PUBLISHED).build();
        Comment parent = Comment.builder().id(9L).post(otherPost).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(9L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(
                1L,
                CreateCommentRequest.builder().content("Reply").parentId(9L).build()))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_BELONG_TO_POST);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createReplyRejectsReplyToReply() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Comment root = Comment.builder().id(8L).post(post).build();
        Comment reply = Comment.builder().id(9L).post(post).parent(root).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(9L)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> commentService.createComment(
                1L,
                CreateCommentRequest.builder().content("Nested").parentId(9L).build()))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_REPLY_TO_REPLY);
    }

    @Test
    void deleteCommentDeletesRepliesBeforeRootComment() {
        TestSecurity.authenticate("admin-1", "ROLE_ADMIN");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Comment root = Comment.builder().id(10L).post(post).userId("student-1").build();
        Comment reply = Comment.builder().id(11L).post(post).parent(root).userId("student-2").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(root));
        when(commentRepository.findByParentId(10L)).thenReturn(List.of(reply));

        commentService.deleteComment(1L, 10L);

        verify(commentRepository).deleteAll(List.of(reply));
        verify(commentRepository).delete(root);
    }

    @Test
    void updateCommentRejectsNonOwner() {
        TestSecurity.authenticate("student-2", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Comment comment = Comment.builder().id(10L).post(post).userId("student-1").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                1L,
                10L,
                UpdateCommentRequest.builder().content("Edit").build()))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_OWNER);
    }
}
