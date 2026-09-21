package com.lms.assignmentservice.service.parser;

import com.lms.assignmentservice.service.QuestionFileReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocxQuestionParser implements QuestionFileParser {

    private final TxtQuestionParser txtQuestionParser;
    private final QuestionFileReader questionFileReader;

    @Override
    public List<ParsedQuestionItem> parse(MultipartFile file) {
        String text = questionFileReader.readDocx(file);
        return txtQuestionParser.parseText(text);
    }

    @Override
    public boolean supports(String extension) {
        return "docx".equalsIgnoreCase(extension);
    }
}
