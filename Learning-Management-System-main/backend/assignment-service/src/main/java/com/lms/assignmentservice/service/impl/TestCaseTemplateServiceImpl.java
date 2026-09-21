package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.enums.TestCaseTemplateFormat;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.QuizTemplateResource;
import com.lms.assignmentservice.service.TestCaseTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TestCaseTemplateServiceImpl implements TestCaseTemplateService {

    @Override
    public QuizTemplateResource getTemplate(String format) {
        TestCaseTemplateFormat templateFormat;
        try {
            String normalizedFormat = format == null ? "xlsx" : format.trim();
            templateFormat = TestCaseTemplateFormat.valueOf(normalizedFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
        }

        var resource = new ClassPathResource(templateFormat.getResourcePath());
        if (!resource.exists()) {
            log.error("Test case template file not found: {}", templateFormat.getResourcePath());
            throw new AssignmentException(ErrorCode.INTERNAL_ERROR);
        }

        return new QuizTemplateResource(toDownloadResource(resource, templateFormat), templateFormat.getFilename(), templateFormat.getMediaType());
    }

    private Resource toDownloadResource(ClassPathResource resource, TestCaseTemplateFormat templateFormat) {
        if (templateFormat != TestCaseTemplateFormat.XLSX) {
            return resource;
        }

        try {
            String base64 = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return new ByteArrayResource(Base64.getMimeDecoder().decode(base64));
        } catch (IOException e) {
            log.error("Cannot read test case template file: {}", templateFormat.getResourcePath(), e);
            throw new AssignmentException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
