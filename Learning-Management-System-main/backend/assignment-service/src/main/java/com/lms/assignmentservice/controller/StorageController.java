package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.DeleteFileRequest;
import com.lms.assignmentservice.dto.request.PresignedUrlRequest;
import com.lms.assignmentservice.dto.response.PresignedUrlResponse;
import com.lms.assignmentservice.service.StorageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorageController {

    StorageService storageService;

    @PostMapping("/presigned-url")
    ApiResponse<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        return ApiResponse.<PresignedUrlResponse>builder()
                .data(storageService.generatePresignedUrl(
                        request.getFileName(), request.getFileType(), request.getFolder()))
                .build();
    }

    @DeleteMapping("/file")
    ApiResponse<Void> deleteFile(@Valid @RequestBody DeleteFileRequest request) {
        storageService.deleteFile(request.getFileUrl());
        return ApiResponse.<Void>builder().build();
    }
}
