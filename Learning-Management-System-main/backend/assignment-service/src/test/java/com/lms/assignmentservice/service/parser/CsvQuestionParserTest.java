package com.lms.assignmentservice.service.parser;

import com.lms.assignmentservice.service.impl.QuestionFileReaderImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvQuestionParserTest {

    CsvQuestionParser parser = new CsvQuestionParser(new QuestionFileReaderImpl());

    @Test
    void parseReturnsRowErrorsInsteadOfDroppingInvalidCsvRecords() {
        String csv = String.join("\n",
                "So thu tu,Noi dung cau hoi,Dap an A,Dap an B,Dap an C,Dap an D,Dap an dung",
                "1,,A,B,C,D,A",
                "2,Missing correct,A,B,C,D,",
                "3,Only one answer,A,,,,A",
                "4,Correct answer has no text,A,B,,,D",
                "5,Valid,A,B,C,D,A");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ParsedQuestionItem> items = parser.parse(file);

        assertThat(items).hasSize(5);
        assertThat(items.subList(0, 4)).allSatisfy(item -> assertThat(item.hasError()).isTrue());
        assertThat(items).extracting(ParsedQuestionItem::getRowNumber).containsExactly(2, 3, 4, 5, 6);
        assertThat(items.get(4).hasError()).isFalse();
        assertThat(items.get(4).getQuestion().getContent()).isEqualTo("Valid");
    }
}
