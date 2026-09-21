package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.response.PresignedUrlResponse;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.service.StorageService;
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
            throw new IdentityException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
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
        } catch (IdentityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {} (type: {}, folder: {}, bucket: {})",
                    fileName, fileType, folder, bucketName, e);
            throw new IdentityException(ErrorCode.FILE_UPLOAD_FAILED);
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

        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted old file from MinIO: {}", objectName);
        } catch (Exception e) {
            log.error("MinIO delete failed for [{}]: {}", fileUrl, e.getMessage(), e);
        }
    }

    @Override
    public void deleteAvatarFileForUser(String fileUrl, String userId) {
        String objectName = extractObjectName(fileUrl);
        if (objectName == null || userId == null || userId.isBlank()) {
            return;
        }

        String allowedPrefix = "avatars/" + userId + "-";
        if (!objectName.startsWith(allowedPrefix)) {
            log.warn("Skip deleting avatar outside current user's prefix: {}", fileUrl);
            return;
        }

        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted discarded avatar from MinIO: {}", objectName);
        } catch (Exception e) {
            log.error("MinIO discarded avatar delete failed for [{}]: {}", fileUrl, e.getMessage(), e);
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
