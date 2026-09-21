package com.lms.blogservice.service.impl;

import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalBlogServiceImplTest {

    @Mock
    PostRepository postRepository;

    @InjectMocks
    InternalBlogServiceImpl internalBlogService;

    @Test
    void getPostReturnsInternalPostContract() {
        Post post = Post.builder()
                .id(1L)
                .title("Java")
                .authorId("author-1")
                .status(PostStatus.PUBLISHED)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        var response = internalBlogService.getPost(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAuthorId()).isEqualTo("author-1");
        assertThat(response.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    void getPostRejectsMissingPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalBlogService.getPost(1L))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void existsPostDelegatesToRepository() {
        when(postRepository.existsById(1L)).thenReturn(true);

        assertThat(internalBlogService.existsPost(1L)).isTrue();
    }
}
