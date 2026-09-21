package com.lms.blogservice.service;

import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.request.UpdateCommentRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {
    Page<CommentResponse> getComments(Long postId, Pageable pageable);
    CommentResponse createComment(Long postId, CreateCommentRequest request);
    CommentResponse updateComment(Long postId, Long commentId,
                                  UpdateCommentRequest request);
    void deleteComment(Long postId, Long commentId);
}