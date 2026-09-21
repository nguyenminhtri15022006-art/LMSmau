package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.configuration.GatewayAuthentication;
import com.lms.assignmentservice.dto.response.PresignedUrlResponse;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.StorageService;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageServiceImpl implements StorageService {

    static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg"
    );

    static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "image/bmp", ".bmp",
            "image/svg+xml", ".svg"
    );

    final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    String bucketName;

    @Value("${minio.url}")
    String minioUrl;

    @Value("${minio.public-url:${minio.url}}")
    String minioPublicUrl;

    @Value("${minio.max-file-size-bytes:5242880}")
    long maxFileSizeBytes;

    @Value("${minio.presigned-expiry-minutes:5}")
    long presignedExpiryMinutes;

    @Override
    public PresignedUrlResponse generatePresignedUrl(String fileName, String fileType, String folder) {
        String ext = extractExtension(fileName, fileType);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new AssignmentException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String userId = GatewayAuthentication.currentUserId();
        String objectName = folder + "/" + userId + "-" + UUID.randomUUID() + ext;

        try {
            PostPolicy policy = new PostPolicy(bucketName,
                    ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(presignedExpiryMinutes));

            policy.addEqualsCondition("key", objectName);
            policy.addContentLengthRangeCondition(1, maxFileSizeBytes);
            policy.addStartsWithCondition("Content-Type", "image/");

            Map<String, String> formFields = new HashMap<>(minioClient.getPresignedPostFormData(policy));
            formFields.put("key", objectName);

            String uploadUrl = minioPublicUrl + "/" + bucketName;
            String publicUrl = minioPublicUrl + "/" + bucketName + "/" + objectName;

            return PresignedUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .formFields(formFields)
                    .publicUrl(publicUrl)
                    .build();
        } catch (AssignmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {} (type: {}, folder: {}, bucket: {})",
                    fileName, fileType, folder, bucketName, e);
            throw new AssignmentException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String objectName = extractObjectName(fileUrl);
        if (objectName == null) {
            return;
        }

        String userId = GatewayAuthentication.currentUserId();
        if (!objectName.startsWith(userId + "-") && !objectName.contains("/" + userId + "-")) {
            log.warn("Skip deleting file not owned by current user: {}", objectName);
            return;
        }

        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted file from MinIO: {}", objectName);
        } catch (Exception e) {
            log.error("MinIO delete failed for [{}]: {}", fileUrl, e.getMessage(), e);
        }
    }

    private String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        String objectName = extractObjectNameWithBaseUrl(fileUrl, minioPublicUrl);
        if (objectName != null) {
            return objectName;
        }

        return extractObjectNameWithBaseUrl(fileUrl, minioUrl);
    }

    private String extractObjectNameWithBaseUrl(String fileUrl, String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }

        String prefix = baseUrl + "/" + bucketName + "/";
        if (!fileUrl.startsWith(prefix)) {
            return null;
        }
        return fileUrl.substring(prefix.length());
    }

    private String extractExtension(String fileName, String fileType) {
        if (fileName != null && fileName.contains(".")) {
            String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp|svg)")) {
                return ext;
            }
        }
        if (fileType != null) {
            return MIME_TO_EXT.getOrDefault(fileType, "");
        }
        return "";
    }
}
