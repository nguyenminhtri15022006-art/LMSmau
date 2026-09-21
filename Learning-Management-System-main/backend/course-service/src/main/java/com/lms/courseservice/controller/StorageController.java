package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.CompleteMultipartRequest;
import com.lms.courseservice.dto.request.InitiateMultipartRequest;
import com.lms.courseservice.dto.response.CompleteMultipartResponse;
import com.lms.courseservice.dto.response.InitiateMultipartResponse;
import com.lms.courseservice.dto.response.PresignedUrlResponse;
import com.lms.courseservice.service.StorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorageController {

    StorageService storageService;

    @PostMapping("/presigned-url")
    ApiResponse<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        return ApiResponse.<PresignedUrlResponse>builder()
                .data(storageService.generatePresignedUrl(request.getFileName(), request.getFileType(), request.getFolder()))
                .build();
    }

    @DeleteMapping("/file")
    ApiResponse<Void> deleteFile(@RequestParam String url) {
        storageService.deleteFile(url);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/multipart/initiate")
    ApiResponse<InitiateMultipartResponse> initiateMultipart(@Valid @RequestBody InitiateMultipartRequest request) {
        return ApiResponse.<InitiateMultipartResponse>builder()
                .data(storageService.initiateMultipartUpload(
                        request.getFileName(), request.getFileType(),
                        request.getFolder(), request.getPartCount()))
                .build();
    }

    @PostMapping("/multipart/complete")
    ApiResponse<CompleteMultipartResponse> completeMultipart(@Valid @RequestBody CompleteMultipartRequest request) {
        return ApiResponse.<CompleteMultipartResponse>builder()
                .data(storageService.completeMultipartUpload(
                        request.getUploadId(), request.getObjectName(), request.getChunkCount()))
                .build();
    }

    @Data
    static class PresignedUrlRequest {
        @NotBlank
        String fileName;
        @NotBlank
        String fileType;
        @Pattern(regexp = "^[a-zA-Z0-9_\\-\\/]+$", message = "INVALID_FOLDER")
        String folder;
    }
}
