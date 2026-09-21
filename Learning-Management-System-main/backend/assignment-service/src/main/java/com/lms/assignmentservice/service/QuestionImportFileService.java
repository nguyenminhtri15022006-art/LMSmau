package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.QuestionImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionImportFileService {
    QuestionImportResultResponse importFile(String courseId, MultipartFile file, String topic);

    QuestionImportResultResponse previewFile(String courseId, MultipartFile file, String topic);
}
