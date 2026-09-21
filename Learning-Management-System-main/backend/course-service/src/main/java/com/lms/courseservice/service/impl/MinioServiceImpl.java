package com.lms.courseservice.service.impl;

import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinioServiceImpl implements MinioService {

    final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    String bucketName;

    @Value("${minio.url}")
    String minioUrl;

    @Value("${minio.public-url:${minio.url}}")
    String minioPublicUrl;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Tạo tên file unique
            String fileName = folder + "/"
                    + UUID.randomUUID()
                    + getExtension(file);

            // Upload lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Trả về URL
            return minioPublicUrl + "/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("MinIO upload failed: {}", e.getMessage(), e);
            throw new CourseException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // Lấy tên file từ URL
            String fileName = extractObjectName(fileUrl);
            if (fileName == null) {
                return;
            }
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO delete failed for [{}]: {}", fileUrl, e.getMessage(), e);
            throw new CourseException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        String fileName = extractObjectNameWithBaseUrl(fileUrl, minioPublicUrl);
        if (fileName != null) {
            return fileName;
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

    private String getExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp|svg)")) {
                return ext;
            }
        }
        // Fallback: derive from content type
        String contentType = file.getContentType();
        if (contentType != null) {
            return switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/gif"  -> ".gif";
                case "image/webp" -> ".webp";
                case "image/bmp"  -> ".bmp";
                case "image/svg+xml" -> ".svg";
                default -> "";
            };
        }
        return "";
    }
}
