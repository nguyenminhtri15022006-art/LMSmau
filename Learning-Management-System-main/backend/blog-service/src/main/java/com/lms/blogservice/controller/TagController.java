package com.lms.blogservice.controller;

import com.lms.blogservice.dto.ApiResponse;
import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.request.UpdateTagRequest;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.service.TagService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TagController {

    TagService tagService;

    @GetMapping
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.<List<TagResponse>>builder()
                .data(tagService.getAllTags())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTagById(@PathVariable Long id) {
        return ApiResponse.<TagResponse>builder()
                .data(tagService.getTagById(id))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> searchTags(
            @RequestParam String keyword) {
        return ApiResponse.<List<TagResponse>>builder()
                .data(tagService.searchTags(keyword))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> createTag(
            @RequestBody @Valid CreateTagRequest request) {
        return ApiResponse.<TagResponse>builder()
                .data(tagService.createTag(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> updateTag(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTagRequest request) {
        return ApiResponse.<TagResponse>builder()
                .data(tagService.updateTag(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ApiResponse.<String>builder()
                .message("Xóa tag thành công")
                .build();
    }
}