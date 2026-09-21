package com.lms.blogservice.controller;

import com.lms.blogservice.dto.ApiResponse;
import com.lms.blogservice.dto.request.CreatePostRequest;
import com.lms.blogservice.dto.request.UpdatePostRequest;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.service.PostService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {

    PostService postService;

    // Public — không cần đăng nhập
    @GetMapping("/public")
    public ApiResponse<Page<PostSummaryResponse>> getPublicPosts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.<Page<PostSummaryResponse>>builder()
                .data(postService.getPosts(pageable,
                        PostStatus.PUBLISHED, tagId, keyword))
                .build();
    }

    @GetMapping("/public/{slug}")
    public ApiResponse<PostResponse> getPostBySlug(@PathVariable String slug) {
        return ApiResponse.<PostResponse>builder()
                .data(postService.getPostBySlug(slug))
                .build();
    }

    // Admin — xem tất cả bài kể cả DRAFT và HIDDEN
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<PostSummaryResponse>> getAllPostsForAdmin(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.<Page<PostSummaryResponse>>builder()
                .data(postService.getAllPostsForAdmin(pageable, status, tagId, keyword))
                .build();
    }

    // Cần đăng nhập
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id) {
        return ApiResponse.<PostResponse>builder()
                .data(postService.getPostById(id))
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<PostSummaryResponse>> getMyPosts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) PostStatus status) {
        return ApiResponse.<Page<PostSummaryResponse>>builder()
                .data(postService.getMyPosts(pageable, status))
                .build();
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PostResponse> createPost(
            @ModelAttribute @Valid CreatePostRequest request) {
        return ApiResponse.<PostResponse>builder()
                .data(postService.createPost(request))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long id,
            @ModelAttribute @Valid UpdatePostRequest request) {
        return ApiResponse.<PostResponse>builder()
                .data(postService.updatePost(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ApiResponse.<String>builder()
                .message("Xóa bài viết thành công")
                .build();
    }
}
