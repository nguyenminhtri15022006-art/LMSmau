package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.enums.QuizTemplateFormat;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.QuizTemplateResource;
import com.lms.assignmentservice.service.QuizTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizTemplateServiceImpl implements QuizTemplateService {

    @Override
    public QuizTemplateResource getTemplate(String format) {
        QuizTemplateFormat templateFormat;
        try {
            templateFormat = QuizTemplateFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
        }

        var resource = new ClassPathResource(templateFormat.getResourcePath());
        if (!resource.exists()) {
            log.error("Template file not found: {}", templateFormat.getResourcePath());
            throw new AssignmentException(ErrorCode.INTERNAL_ERROR);
        }

        return new QuizTemplateResource(resource, templateFormat.getFilename(), templateFormat.getMediaType());
    }
}
