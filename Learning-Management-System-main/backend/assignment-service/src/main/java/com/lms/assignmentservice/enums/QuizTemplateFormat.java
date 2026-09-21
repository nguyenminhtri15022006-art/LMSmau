package com.lms.assignmentservice.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum QuizTemplateFormat {
    DOCX("templates/quiz-import/quiz-template.docx", "quiz-template.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    TXT("templates/quiz-import/quiz-template.txt", "quiz-template.txt", "text/plain"),
    CSV("templates/quiz-import/quiz-template.csv", "quiz-template.csv", "text/csv");

    String resourcePath;
    String filename;
    String mediaType;
}
