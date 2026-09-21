package com.lms.identityservice.service;

import com.lms.identityservice.dto.response.PresignedUrlResponse;

public interface StorageService {
    PresignedUrlResponse generatePresignedUrl(String fileName, String fileType, String folder);
    void deleteFile(String fileUrl);
    void deleteAvatarFileForUser(String fileUrl, String userId);
}
