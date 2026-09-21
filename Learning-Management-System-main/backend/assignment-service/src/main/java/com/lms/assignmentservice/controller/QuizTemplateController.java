package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.service.QuizTemplateResource;
import com.lms.assignmentservice.service.QuizTemplateService;
import com.lms.assignmentservice.service.TestCaseTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizTemplateController {
    QuizTemplateService quizTemplateService;
    TestCaseTemplateService testCaseTemplateService;

    @GetMapping("/quiz-import")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Resource> downloadQuizImportTemplate(@RequestParam(defaultValue = "docx") String format) {
        QuizTemplateResource template = quizTemplateService.getTemplate(format);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(template.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(template.resource());
    }

    @GetMapping("/testcases")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Resource> downloadTestCaseTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        QuizTemplateResource template = testCaseTemplateService.getTemplate(format);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(template.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(template.resource());
    }
}
