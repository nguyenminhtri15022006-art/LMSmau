package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.response.InternalPostResponse;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.service.InternalBlogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalBlogServiceImpl implements InternalBlogService {

    PostRepository postRepository;

    @Override
    public InternalPostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        return InternalPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .authorId(post.getAuthorId())
                .status(post.getStatus())
                .build();
    }

    @Override
    public boolean existsPost(Long postId) {
        return postRepository.existsById(postId);
    }
}
