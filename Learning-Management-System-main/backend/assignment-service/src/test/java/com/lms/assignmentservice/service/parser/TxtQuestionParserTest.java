package com.lms.assignmentservice.service.parser;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.service.QuestionFileReader;
import com.lms.assignmentservice.service.impl.QuestionFileReaderImpl;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TxtQuestionParserTest {

    QuestionFileReader questionFileReader = new QuestionFileReaderImpl();
    TxtQuestionParser parser = new TxtQuestionParser(questionFileReader);

    @Test
    void parseTextAcceptsNewQuestionAndAnswerFormat() {
        String content = String.join("\n",
                "Câu 1: What is Java?",
                "A. A database",
                "*B. A programming language",
                "C. A spreadsheet",
                "D. A browser",
                "",
                "Câu 2: Select prime numbers",
                "*A. 2",
                "*B. 3",
                "C. 4",
                "D. 6");

        List<ParsedQuestionItem> items = parser.parseText(content);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getQuestionNumber()).isEqualTo(1);
        assertThat(items.get(0).getQuestion().getContent()).isEqualTo("What is Java?");
        assertThat(items.get(0).getQuestion().getType()).isEqualTo(QuestionType.SINGLE);
        assertThat(items.get(0).getQuestion().getAnswers())
                .extracting(CreateQuestionRequest.AnswerRequest::getContent)
                .containsExactly("A database", "A programming language", "A spreadsheet", "A browser");
        assertThat(items.get(0).getQuestion().getAnswers())
                .extracting(CreateQuestionRequest.AnswerRequest::isCorrect)
                .containsExactly(false, true, false, false);

        assertThat(items.get(1).getQuestionNumber()).isEqualTo(2);
        assertThat(items.get(1).getQuestion().getType()).isEqualTo(QuestionType.MULTIPLE);
        assertThat(items.get(1).getQuestion().getAnswers())
                .extracting(CreateQuestionRequest.AnswerRequest::isCorrect)
                .containsExactly(true, true, false, false);
    }

    @Test
    void docxParserUsesSameNewTextFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes(List.of(
                        "Câu 1: What is Spring?",
                        "A. A season only",
                        "*B. A Java framework",
                        "C. A database",
                        "D. An IDE")));
        DocxQuestionParser docxParser = new DocxQuestionParser(parser, questionFileReader);

        List<ParsedQuestionItem> items = docxParser.parse(file);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getQuestionNumber()).isEqualTo(1);
        assertThat(items.getFirst().getQuestion().getContent()).isEqualTo("What is Spring?");
        assertThat(items.getFirst().getQuestion().getAnswers())
                .extracting(CreateQuestionRequest.AnswerRequest::isCorrect)
                .containsExactly(false, true, false, false);
    }

    @Test
    void parseTextDoesNotAcceptLegacyNumberAndParenthesisFormat() {
        String content = String.join("\n",
                "1. Legacy question",
                "A) Wrong",
                "*B) Correct",
                "C) Other",
                "D) Last");

        List<ParsedQuestionItem> items = parser.parseText(content);

        assertThat(items).isEmpty();
    }

    @Test
    void parseTextLeavesMissingCorrectAnswerForImportValidation() {
        String content = String.join("\n",
                "Câu 1: Missing correct answer",
                "A. Option A",
                "B. Option B",
                "C. Option C",
                "D. Option D");

        List<ParsedQuestionItem> items = parser.parseText(content);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getQuestion().getType()).isEqualTo(QuestionType.SINGLE);
        assertThat(items.getFirst().getQuestion().getAnswers())
                .extracting(CreateQuestionRequest.AnswerRequest::isCorrect)
                .containsExactly(false, false, false, false);
    }

    private byte[] docxBytes(List<String> paragraphs) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String text : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(text);
            }
            document.write(out);
            return out.toByteArray();
        }
    }
}
