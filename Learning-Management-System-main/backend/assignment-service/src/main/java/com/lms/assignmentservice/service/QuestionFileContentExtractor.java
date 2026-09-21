package com.lms.assignmentservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface QuestionFileContentExtractor {
    String extract(MultipartFile file);
}
