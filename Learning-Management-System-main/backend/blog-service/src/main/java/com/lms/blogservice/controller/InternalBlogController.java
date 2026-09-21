package com.lms.blogservice.controller;

import com.lms.blogservice.dto.ApiResponse;
import com.lms.blogservice.dto.response.InternalPostResponse;
import com.lms.blogservice.service.InternalBlogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalBlogController {

    InternalBlogService internalBlogService;

    @GetMapping("/posts/{postId}")
    public ApiResponse<InternalPostResponse> getPost(
            @PathVariable Long postId) {
        return ApiResponse.<InternalPostResponse>builder()
                .data(internalBlogService.getPost(postId))
                .build();
    }

    @GetMapping("/posts/{postId}/exists")
    public ApiResponse<Boolean> existsPost(@PathVariable Long postId) {
        return ApiResponse.<Boolean>builder()
                .data(internalBlogService.existsPost(postId))
                .build();
    }
}
