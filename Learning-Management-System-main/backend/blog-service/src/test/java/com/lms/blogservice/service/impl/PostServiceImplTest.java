package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.request.CreatePostRequest;
import com.lms.blogservice.dto.request.UpdatePostRequest;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.entity.Tag;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.PostMapper;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.TagRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.service.MinioService;
import com.lms.blogservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    PostRepository postRepository;
    @Mock
    TagRepository tagRepository;
    @Mock
    CommentRepository commentRepository;
    @Mock
    VoteRepository voteRepository;
    @Mock
    PostMapper postMapper;
    @Mock
    MinioService minioService;

    @InjectMocks
    PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "thumbnailFolder", "thumbnails");
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void getPostsAlwaysQueriesPublishedPostsAndAddsCounts() {
        Post post = Post.builder().id(1L).title("Java").status(PostStatus.PUBLISHED).build();
        when(postRepository.findByStatus(PostStatus.PUBLISHED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(postMapper.toPostSummaryResponse(post)).thenReturn(PostSummaryResponse.builder().id(1L).title("Java").build());
        when(commentRepository.countByPostId(1L)).thenReturn(2L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.UPVOTE)).thenReturn(3L);

        var page = postService.getPosts(PageRequest.of(0, 10), PostStatus.HIDDEN, null, null);

        assertThat(page.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getCommentCount()).isEqualTo(2);
            assertThat(response.getUpvoteCount()).isEqualTo(3);
        });
    }

    @Test
    void getPostBySlugIncrementsViewForPublishedPostAndAddsCounts() {
        Post post = Post.builder()
                .id(1L)
                .slug("java")
                .title("Java")
                .authorId("author-1")
                .status(PostStatus.PUBLISHED)
                .viewCount(4L)
                .build();
        when(postRepository.findBySlug("java")).thenReturn(Optional.of(post));
        when(postMapper.toPostResponse(post)).thenReturn(PostResponse.builder().id(1L).viewCount(5L).build());
        when(commentRepository.countByPostId(1L)).thenReturn(1L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.UPVOTE)).thenReturn(2L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.DOWNVOTE)).thenReturn(1L);

        PostResponse response = postService.getPostBySlug("java");

        assertThat(post.getViewCount()).isEqualTo(5);
        assertThat(response.getCommentCount()).isEqualTo(1);
        assertThat(response.getUpvoteCount()).isEqualTo(2);
        assertThat(response.getDownvoteCount()).isEqualTo(1);
        verify(postRepository).save(post);
    }

    @Test
    void getPostBySlugHidesDraftFromNonOwnerAsNotFound() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder()
                .id(1L)
                .slug("draft")
                .authorId("author-1")
                .status(PostStatus.DRAFT)
                .build();
        when(postRepository.findBySlug("draft")).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPostBySlug("draft"))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postRepository, never()).save(any());
    }

    @Test
    void createPostSetsAuthorSlugThumbnailAndTags() {
        TestSecurity.authenticate("author-1", "ROLE_STUDENT");
        MockMultipartFile thumbnail = new MockMultipartFile("thumbnail", "cover.png", "image/png", new byte[]{1});
        CreatePostRequest request = CreatePostRequest.builder()
                .title("Java Basics")
                .content("content")
                .summary("summary")
                .status(PostStatus.PUBLISHED)
                .thumbnail(thumbnail)
                .tagIds(List.of(10L))
                .build();
        Post mapped = Post.builder().title("Java Basics").content("content").status(PostStatus.PUBLISHED).build();
        Tag tag = Tag.builder().id(10L).name("Java").slug("java").build();

        when(postRepository.existsByTitle("Java Basics")).thenReturn(false);
        when(postRepository.existsBySlug("java-basics")).thenReturn(false);
        when(postMapper.toPost(request)).thenReturn(mapped);
        when(minioService.uploadFile(thumbnail, "thumbnails")).thenReturn("https://cdn/cover.png");
        when(tagRepository.findAllById(List.of(10L))).thenReturn(List.of(tag));
        when(postRepository.save(mapped)).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1L);
            return post;
        });
        when(postMapper.toPostResponse(mapped)).thenReturn(PostResponse.builder().id(1L).title("Java Basics").build());

        PostResponse response = postService.createPost(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(mapped.getAuthorId()).isEqualTo("author-1");
        assertThat(mapped.getSlug()).isEqualTo("java-basics");
        assertThat(mapped.getThumbnail()).isEqualTo("https://cdn/cover.png");
        assertThat(mapped.getTags()).containsExactly(tag);
    }

    @Test
    void updatePostRejectsNonOwner() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Post post = Post.builder()
                .id(1L)
                .title("Java")
                .authorId("author-1")
                .status(PostStatus.PUBLISHED)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(1L, updateRequest("Java")))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_OWNER);
    }

    @Test
    void deletePostAllowsAdminAndDeletesThumbnail() {
        TestSecurity.authenticate("admin-1", "ROLE_ADMIN");
        Post post = Post.builder()
                .id(1L)
                .authorId("author-1")
                .thumbnail("https://cdn/cover.png")
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.deletePost(1L);

        verify(minioService).deleteFile("https://cdn/cover.png");
        verify(postRepository).delete(post);
    }

    @Test
    void updatePostClearsTagsWhenRequested() {
        TestSecurity.authenticate("author-1", "ROLE_STUDENT");
        Tag tag = Tag.builder().id(10L).name("Java").slug("java").build();
        Post post = Post.builder()
                .id(1L)
                .title("Java")
                .authorId("author-1")
                .status(PostStatus.PUBLISHED)
                .tags(new java.util.ArrayList<>(List.of(tag)))
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(postMapper.toPostResponse(post)).thenReturn(PostResponse.builder().id(1L).build());

        postService.updatePost(1L, updateRequest("Java"));

        assertThat(post.getTags()).isEmpty();
        verify(tagRepository, never()).findAllById(any());
    }

    private UpdatePostRequest updateRequest(String title) {
        return UpdatePostRequest.builder()
                .title(title)
                .content("content")
                .status(PostStatus.PUBLISHED)
                .clearTags(true)
                .build();
    }
}
