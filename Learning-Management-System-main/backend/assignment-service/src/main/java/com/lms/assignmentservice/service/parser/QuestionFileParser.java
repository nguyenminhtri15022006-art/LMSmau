package com.lms.assignmentservice.service.parser;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionFileParser {
    List<ParsedQuestionItem> parse(MultipartFile file);

    boolean supports(String extension);
}
