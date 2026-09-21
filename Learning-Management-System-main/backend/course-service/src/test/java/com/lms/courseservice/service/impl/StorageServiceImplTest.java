package com.lms.courseservice.service.impl;

import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.service.MinioService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @Mock
    MinioClient publicMinioClient;
    @Mock
    MinioClient internalMinioClient;
    @Mock
    MinioService minioService;

    StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageServiceImpl(publicMinioClient, internalMinioClient, minioService);
        ReflectionTestUtils.setField(storageService, "bucketName", "lms");
        ReflectionTestUtils.setField(storageService, "minioPublicUrl", "https://cdn.example.com");
        ReflectionTestUtils.setField(storageService, "presignedExpiryMinutes", 5);
    }

    @Test
    void generatePresignedUrlUsesFileExtensionAndPublicUrl() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio/upload");

        var response = storageService.generatePresignedUrl("intro.mp4", "video/mp4", "lessons");

        assertThat(response.getPresignedUrl()).isEqualTo("https://minio/upload");
        assertThat(response.getPublicUrl()).startsWith("https://cdn.example.com/lms/lessons/");
        assertThat(response.getPublicUrl()).endsWith(".mp4");
    }

    @Test
    void generatePresignedUrlFallsBackToMimeTypeExtension() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio/upload");

        var response = storageService.generatePresignedUrl("avatar", "image/png", "avatars");

        assertThat(response.getPublicUrl()).endsWith(".png");
    }

    @Test
    void generatePresignedUrlWrapsMinioFailureAsCourseException() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        assertThatThrownBy(() -> storageService.generatePresignedUrl("intro.mp4", "video/mp4", "lessons"))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void deleteFileDelegatesToMinioService() {
        storageService.deleteFile("https://cdn.example.com/lms/lessons/file.mp4");

        verify(minioService).deleteFile("https://cdn.example.com/lms/lessons/file.mp4");
    }
}
