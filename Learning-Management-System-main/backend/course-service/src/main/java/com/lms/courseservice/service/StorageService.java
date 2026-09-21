package com.lms.courseservice.service;

import com.lms.courseservice.dto.response.CompleteMultipartResponse;
import com.lms.courseservice.dto.response.InitiateMultipartResponse;
import com.lms.courseservice.dto.response.PresignedUrlResponse;

public interface StorageService {
    PresignedUrlResponse generatePresignedUrl(String fileName, String fileType, String folder);
    void deleteFile(String fileUrl);
    InitiateMultipartResponse initiateMultipartUpload(String fileName, String fileType, String folder, int partCount);
    CompleteMultipartResponse completeMultipartUpload(String uploadId, String objectName, int chunkCount);
}
