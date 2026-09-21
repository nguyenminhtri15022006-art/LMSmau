package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.entity.Comment;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.entity.Vote;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock
    PostRepository postRepository;
    @Mock
    CommentRepository commentRepository;
    @Mock
    VoteRepository voteRepository;

    @InjectMocks
    VoteServiceImpl voteService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void votePostRejectsOwnPost() {
        TestSecurity.authenticate("author-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).authorId("author-1").status(PostStatus.PUBLISHED).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> voteService.votePost(1L, VoteRequest.builder().voteType(VoteType.UPVOTE).build()))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_VOTE_OWN_POST);
    }

    @Test
    void votePostCreatesNewVoteAndReturnsCounts() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).authorId("author-1").status(PostStatus.PUBLISHED).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType("student-1", 1L, TargetType.POST))
                .thenReturn(Optional.empty());
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.UPVOTE)).thenReturn(1L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.DOWNVOTE)).thenReturn(0L);

        var response = voteService.votePost(1L, VoteRequest.builder().voteType(VoteType.UPVOTE).build());

        assertThat(response.getVoteType()).isEqualTo(VoteType.UPVOTE);
        assertThat(response.getUpvoteCount()).isEqualTo(1);
        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("student-1");
        assertThat(captor.getValue().getTargetType()).isEqualTo(TargetType.POST);
    }

    @Test
    void votePostTogglesSameVoteOff() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).authorId("author-1").status(PostStatus.PUBLISHED).build();
        Vote existing = Vote.builder().id(99L).userId("student-1").targetId(1L).targetType(TargetType.POST).voteType(VoteType.UPVOTE).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType("student-1", 1L, TargetType.POST))
                .thenReturn(Optional.of(existing));

        var response = voteService.votePost(1L, VoteRequest.builder().voteType(VoteType.UPVOTE).build());

        assertThat(response.getVoteType()).isNull();
        verify(voteRepository).delete(existing);
    }

    @Test
    void voteCommentRejectsCommentFromAnotherPost() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Post otherPost = Post.builder().id(2L).status(PostStatus.PUBLISHED).build();
        Comment comment = Comment.builder().id(10L).post(otherPost).userId("student-2").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> voteService.voteComment(
                1L,
                10L,
                VoteRequest.builder().voteType(VoteType.DOWNVOTE).build()))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_BELONG_TO_POST);
    }

    @Test
    void voteCommentRejectsOwnComment() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Comment comment = Comment.builder().id(10L).post(post).userId("student-1").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> voteService.voteComment(
                1L,
                10L,
                VoteRequest.builder().voteType(VoteType.UPVOTE).build()))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_VOTE_OWN_COMMENT);
    }

    @Test
    void voteCommentChangesExistingVoteType() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder().id(1L).status(PostStatus.PUBLISHED).build();
        Comment comment = Comment.builder().id(10L).post(post).userId("student-2").build();
        Vote existing = Vote.builder()
                .id(99L)
                .userId("student-1")
                .targetId(10L)
                .targetType(TargetType.COMMENT)
                .voteType(VoteType.DOWNVOTE)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType("student-1", 10L, TargetType.COMMENT))
                .thenReturn(Optional.of(existing));

        var response = voteService.voteComment(1L, 10L, VoteRequest.builder().voteType(VoteType.UPVOTE).build());

        assertThat(response.getVoteType()).isEqualTo(VoteType.UPVOTE);
        assertThat(existing.getVoteType()).isEqualTo(VoteType.UPVOTE);
        verify(voteRepository).save(existing);
    }
}
