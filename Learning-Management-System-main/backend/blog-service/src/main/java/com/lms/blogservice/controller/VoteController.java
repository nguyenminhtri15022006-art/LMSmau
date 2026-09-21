package com.lms.blogservice.controller;

import com.lms.blogservice.dto.ApiResponse;
import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.dto.response.VoteResponse;
import com.lms.blogservice.service.VoteService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteController {

    VoteService voteService;

    @PostMapping("/posts/{postId}/vote")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<VoteResponse> votePost(
            @PathVariable Long postId,
            @RequestBody @Valid VoteRequest request) {
        return ApiResponse.<VoteResponse>builder()
                .data(voteService.votePost(postId, request))
                .build();
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/vote")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<VoteResponse> voteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid VoteRequest request) {
        return ApiResponse.<VoteResponse>builder()
                .data(voteService.voteComment(postId, commentId, request))
                .build();
    }
}
