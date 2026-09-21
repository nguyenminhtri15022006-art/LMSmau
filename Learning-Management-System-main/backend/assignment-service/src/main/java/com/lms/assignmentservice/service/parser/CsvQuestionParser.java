package com.lms.assignmentservice.service.parser;

import com.lms.assignmentservice.dto.request.CreateQuestionRequest;
import com.lms.assignmentservice.enums.QuestionType;
import com.lms.assignmentservice.service.QuestionFileReader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CsvQuestionParser implements QuestionFileParser {

    private static final String[] EXPECTED_HEADERS = {
            "So thu tu", "Noi dung cau hoi",
            "Dap an A", "Dap an B", "Dap an C", "Dap an D",
            "Dap an dung"
    };

    private final QuestionFileReader questionFileReader;

    @Override
    public List<ParsedQuestionItem> parse(MultipartFile file) {
        List<ParsedQuestionItem> items = new ArrayList<>();

        try {
            for (CSVRecord record : questionFileReader.readCsvRecords(file)) {
                items.add(parseRow(record));
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot read CSV question file with headers: "
                    + String.join(", ", EXPECTED_HEADERS), e);
        }

        return items;
    }

    @Override
    public boolean supports(String extension) {
        return "csv".equalsIgnoreCase(extension);
    }

    private ParsedQuestionItem parseRow(CSVRecord record) {
        int rowNumber = (int) record.getRecordNumber() + 1;
        String rawText = record.toString();
        String questionText = record.get("Noi dung cau hoi");
        if (questionText == null || questionText.isBlank()) {
            return errorItem(rowNumber, "Noi dung cau hoi khong duoc de trong", rawText);
        }

        String correctAnswerStr = record.get("Dap an dung");
        if (correctAnswerStr == null || correctAnswerStr.isBlank()) {
            return errorItem(rowNumber, "Dap an dung khong duoc de trong", rawText);
        }

        String[] correctLabels = correctAnswerStr.split(",");
        Set<String> correctList = new LinkedHashSet<>();
        for (String label : correctLabels) {
            String trimmed = label.trim().toUpperCase();
            if (!trimmed.isEmpty()) {
                correctList.add(trimmed);
            }
        }

        if (correctList.isEmpty()) {
            return errorItem(rowNumber, "Dap an dung khong duoc de trong", rawText);
        }

        CreateQuestionRequest question = new CreateQuestionRequest();
        question.setContent(questionText);
        question.setScore((byte) 10);

        List<CreateQuestionRequest.AnswerRequest> answers = new ArrayList<>();
        String[] labels = {"A", "B", "C", "D"};

        for (String label : labels) {
            String answerText = record.get("Dap an " + label);
            if (answerText == null || answerText.isBlank()) {
                continue;
            }

            CreateQuestionRequest.AnswerRequest answer = new CreateQuestionRequest.AnswerRequest();
            answer.setContent(answerText);
            answer.setCorrect(correctList.contains(label));
            answer.setOrderIndex((short) (label.charAt(0) - 'A'));
            answers.add(answer);
        }

        if (answers.size() < 2) {
            return errorItem(rowNumber, "Phai co it nhat 2 dap an co noi dung", rawText);
        }

        boolean allCorrectLabelsValid = correctList.stream()
                .allMatch(label -> label.length() == 1 && label.charAt(0) >= 'A' && label.charAt(0) <= 'D');
        boolean allCorrectAnswersExist = correctList.stream()
                .allMatch(label -> answers.stream().anyMatch(answer ->
                        answer.getOrderIndex() == (short) (label.charAt(0) - 'A')));
        if (!allCorrectLabelsValid || !allCorrectAnswersExist) {
            return errorItem(rowNumber, "Dap an dung khong nam trong cac dap an co noi dung", rawText);
        }

        question.setAnswers(answers);
        question.setType(correctList.size() > 1 ? QuestionType.MULTIPLE : QuestionType.SINGLE);
        return ParsedQuestionItem.builder()
                .rowNumber(rowNumber)
                .questionNumber(rowNumber - 1)
                .rawText(rawText)
                .question(question)
                .build();
    }

    private ParsedQuestionItem errorItem(int rowNumber, String message, String rawText) {
        return ParsedQuestionItem.builder()
                .rowNumber(rowNumber)
                .questionNumber(rowNumber - 1)
                .rawText(rawText)
                .errorMessage(message)
                .build();
    }
}
