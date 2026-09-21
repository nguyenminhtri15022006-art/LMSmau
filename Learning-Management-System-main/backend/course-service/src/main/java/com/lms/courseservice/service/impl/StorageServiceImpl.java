package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.response.CompleteMultipartResponse;
import com.lms.courseservice.dto.response.InitiateMultipartResponse;
import com.lms.courseservice.dto.response.PresignedUrlResponse;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.service.MinioService;
import com.lms.courseservice.service.StorageService;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageServiceImpl implements StorageService {

    final MinioClient publicMinioClient;
    final MinioClient internalMinioClient;
    final MinioService minioService;

    public StorageServiceImpl(@Qualifier("publicMinioClient") MinioClient publicMinioClient,
                              MinioClient internalMinioClient,
                              MinioService minioService) {
        this.publicMinioClient = publicMinioClient;
        this.internalMinioClient = internalMinioClient;
        this.minioService = minioService;
    }

    @Value("${minio.bucket-name}")
    String bucketName;

    @Value("${minio.url}")
    String minioUrl;

    @Value("${minio.public-url:${minio.url}}")
    String minioPublicUrl;

    @Value("${minio.presigned-expiry-minutes:60}")
    int presignedExpiryMinutes;

    @Override
    public PresignedUrlResponse generatePresignedUrl(String fileName, String fileType, String folder) {
        try {
            String ext = extractExtension(fileName, fileType);
            String objectName = folder + "/" + UUID.randomUUID() + ext;

            String presignedUrl = publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(presignedExpiryMinutes, TimeUnit.MINUTES)
                            .build()
            );

            String publicUrl = minioPublicUrl + "/" + bucketName + "/" + objectName;

            return PresignedUrlResponse.builder()
                    .presignedUrl(presignedUrl)
                    .publicUrl(publicUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: file={}, folder={}", fileName, folder, e);
            throw new CourseException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        minioService.deleteFile(fileUrl);
    }

    /**
     * Tạo presigned PUT URL cho từng chunk.
     * Mỗi chunk được upload thành một object riêng trong temp-parts/{uploadId}/.
     * completeMultipartUpload sẽ dùng composeObject để ghép lại.
     */
    @Override
    public InitiateMultipartResponse initiateMultipartUpload(String fileName, String fileType,
                                                              String folder, int partCount) {
        try {
            String uploadId = UUID.randomUUID().toString();
            String ext = extractExtension(fileName, fileType);
            String finalObjectName = folder + "/" + UUID.randomUUID() + ext;

            List<String> presignedUrls = new ArrayList<>(partCount);
            for (int i = 0; i < partCount; i++) {
                String chunkObject = chunkObjectName(uploadId, i, ext);
                String url = publicMinioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(bucketName)
                                .object(chunkObject)
                                .expiry(presignedExpiryMinutes, TimeUnit.MINUTES)
                                .build()
                );
                presignedUrls.add(url);
            }

            log.info("Initiated multipart upload: uploadId={}, parts={}, dest={}", uploadId, partCount, finalObjectName);
            return InitiateMultipartResponse.builder()
                    .uploadId(uploadId)
                    .objectName(finalObjectName)
                    .presignedUrls(presignedUrls)
                    .build();
        } catch (Exception e) {
            log.error("Failed to initiate multipart upload: file={}, folder={}", fileName, folder, e);
            throw new CourseException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Ghép các chunk thành object cuối cùng bằng composeObject,
     * sau đó xóa các chunk tạm.
     */
    @Override
    public CompleteMultipartResponse completeMultipartUpload(String uploadId, String objectName, int chunkCount) {
        String ext = objectName.contains(".") ? objectName.substring(objectName.lastIndexOf(".")) : "";
        try {
            List<ComposeSource> sources = new ArrayList<>(chunkCount);
            for (int i = 0; i < chunkCount; i++) {
                sources.add(ComposeSource.builder()
                        .bucket(bucketName)
                        .object(chunkObjectName(uploadId, i, ext))
                        .build());
            }

            internalMinioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .sources(sources)
                            .build()
            );

            log.info("Composed {} chunks into: {}", chunkCount, objectName);
        } catch (Exception e) {
            log.error("Failed to compose multipart upload: uploadId={}, object={}", uploadId, objectName, e);
            throw new CourseException(ErrorCode.INTERNAL_ERROR);
        }

        // Xóa chunk tạm (best-effort, không throw nếu lỗi)
        for (int i = 0; i < chunkCount; i++) {
            String chunk = chunkObjectName(uploadId, i, ext);
            try {
                internalMinioClient.removeObject(
                        RemoveObjectArgs.builder().bucket(bucketName).object(chunk).build()
                );
            } catch (Exception e) {
                log.warn("Failed to delete chunk: {}", chunk);
            }
        }

        return CompleteMultipartResponse.builder()
                .publicUrl(minioPublicUrl + "/" + bucketName + "/" + objectName)
                .build();
    }

    private String chunkObjectName(String uploadId, int index, String ext) {
        return "temp-parts/" + uploadId + "/part-" + index + ext;
    }

    private String extractExtension(String fileName, String fileType) {
        if (fileName != null && fileName.contains(".")) {
            String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp|svg|mp4|mov|webm|avi|mkv|pdf|doc|docx|ppt|pptx|xls|xlsx|zip|txt)")) {
                return ext;
            }
        }
        if (fileType != null) {
            return switch (fileType) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/gif"  -> ".gif";
                case "image/webp" -> ".webp";
                case "image/bmp"  -> ".bmp";
                case "image/svg+xml" -> ".svg";
                case "video/mp4"  -> ".mp4";
                case "video/quicktime" -> ".mov";
                case "video/webm" -> ".webm";
                case "video/x-msvideo" -> ".avi";
                case "video/x-matroska" -> ".mkv";
                case "application/pdf" -> ".pdf";
                case "application/msword" -> ".doc";
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
                case "application/vnd.ms-powerpoint" -> ".ppt";
                case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
                default -> "";
            };
        }
        return "";
    }
}
