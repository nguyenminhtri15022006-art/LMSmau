package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.PresignedUrlResponse;

public interface StorageService {
    PresignedUrlResponse generatePresignedUrl(String fileName, String fileType, String folder);
    void deleteFile(String fileUrl);
}
