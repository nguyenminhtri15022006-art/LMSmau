package com.lms.blogservice.service;

import com.lms.blogservice.dto.response.InternalPostResponse;

public interface InternalBlogService {
    InternalPostResponse getPost(Long postId);
    boolean existsPost(Long postId);
}