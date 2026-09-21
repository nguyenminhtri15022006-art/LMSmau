package com.lms.assignmentservice.service.parser;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.service.QuestionFileReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TxtQuestionParser implements QuestionFileParser {

    private static final Pattern QUESTION_START = Pattern.compile(
            "^C\u00e2u\\s+(\\d+)\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ANSWER_LINE = Pattern.compile("^(\\*)?([A-D])\\.\\s+(.+)$");

    private final QuestionFileReader questionFileReader;

    @Override
    public List<ParsedQuestionItem> parse(MultipartFile file) {
        String content = questionFileReader.readTxt(file);
        return parseText(content);
    }

    @Override
    public boolean supports(String extension) {
        return "txt".equalsIgnoreCase(extension);
    }

    /**
     * Parse text content into import items. Shared by TXT and DOCX parsers.
     */
    public List<ParsedQuestionItem> parseText(String content) {
        List<ParsedQuestionItem> items = new ArrayList<>();
        String[] lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n");

        CreateQuestionRequest currentQuestion = null;
        List<CreateQuestionRequest.AnswerRequest> currentAnswers = null;
        String currentQuestionText = null;
        StringBuilder currentRawText = new StringBuilder();
        int questionNumber = 0;
        int currentRowNumber = 0;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (currentQuestion != null && currentQuestionText != null) {
                    addItem(items, currentQuestion, currentAnswers, currentRawText, currentRowNumber, questionNumber);
                }
                currentQuestion = null;
                currentAnswers = null;
                currentQuestionText = null;
                currentRawText = new StringBuilder();
                currentRowNumber = 0;
                continue;
            }

            Matcher questionMatch = QUESTION_START.matcher(trimmed);
            if (questionMatch.matches()) {
                if (currentQuestion != null && currentQuestionText != null) {
                    addItem(items, currentQuestion, currentAnswers, currentRawText, currentRowNumber, questionNumber);
                }
                questionNumber = Integer.parseInt(questionMatch.group(1));
                currentQuestionText = questionMatch.group(2);
                currentQuestion = new CreateQuestionRequest();
                currentQuestion.setContent(currentQuestionText);
                currentQuestion.setType(QuestionType.SINGLE);
                currentAnswers = new ArrayList<>();
                currentRawText = new StringBuilder(line);
                currentRowNumber = lineIndex + 1;
                continue;
            }

            Matcher answerMatch = ANSWER_LINE.matcher(trimmed);
            if (answerMatch.matches() && currentQuestion != null) {
                currentRawText.append("\n").append(line);
                boolean correct = answerMatch.group(1) != null;
                String label = answerMatch.group(2);
                String text = answerMatch.group(3);

                CreateQuestionRequest.AnswerRequest answer = new CreateQuestionRequest.AnswerRequest();
                answer.setContent(text);
                answer.setCorrect(correct);
                answer.setOrderIndex((short) (label.charAt(0) - 'A'));
                currentAnswers.add(answer);
            }
        }

        if (currentQuestion != null && currentQuestionText != null) {
            addItem(items, currentQuestion, currentAnswers, currentRawText, currentRowNumber, questionNumber);
        }

        return items;
    }

    private void addItem(
            List<ParsedQuestionItem> items,
            CreateQuestionRequest question,
            List<CreateQuestionRequest.AnswerRequest> answers,
            StringBuilder rawText,
            int rowNumber,
            int questionNumber
    ) {
        question.setAnswers(answers);
        long correctCount = answers.stream().filter(CreateQuestionRequest.AnswerRequest::isCorrect).count();
        question.setType(correctCount > 1 ? QuestionType.MULTIPLE : QuestionType.SINGLE);
        question.setScore((byte) 10);
        items.add(ParsedQuestionItem.builder()
                .rowNumber(rowNumber)
                .questionNumber(questionNumber)
                .rawText(rawText.toString())
                .question(question)
                .build());
    }
}
