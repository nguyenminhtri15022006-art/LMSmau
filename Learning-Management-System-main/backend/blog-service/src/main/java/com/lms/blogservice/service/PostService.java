package com.lms.blogservice.service;

import com.lms.blogservice.dto.request.CreatePostRequest;
import com.lms.blogservice.dto.request.UpdatePostRequest;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Page<PostSummaryResponse> getPosts(Pageable pageable, PostStatus status,
                                       Long tagId, String keyword);
    Page<PostSummaryResponse> getAllPostsForAdmin(Pageable pageable, PostStatus status,
                                                  Long tagId, String keyword);
    Page<PostSummaryResponse> getMyPosts(Pageable pageable, PostStatus status);
    PostResponse getPostById(Long id);
    PostResponse getPostBySlug(String slug);
    PostResponse createPost(CreatePostRequest request);
    PostResponse updatePost(Long id, UpdatePostRequest request);
    void deletePost(Long id);
}
