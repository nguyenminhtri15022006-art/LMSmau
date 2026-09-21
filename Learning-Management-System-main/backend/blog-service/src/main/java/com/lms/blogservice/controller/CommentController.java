package com.lms.blogservice.controller;

import com.lms.blogservice.dto.ApiResponse;
import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.request.UpdateCommentRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.service.CommentService;
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
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {

    CommentService commentService;

    @GetMapping
    public ApiResponse<Page<CommentResponse>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<CommentResponse>>builder()
                .data(commentService.getComments(postId, pageable))
                .build();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CreateCommentRequest request) {
        return ApiResponse.<CommentResponse>builder()
                .data(commentService.createComment(postId, request))
                .build();
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request) {
        return ApiResponse.<CommentResponse>builder()
                .data(commentService.updateComment(postId, commentId, request))
                .build();
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.deleteComment(postId, commentId);
        return ApiResponse.<String>builder()
                .message("Xóa bình luận thành công")
                .build();
    }
}
