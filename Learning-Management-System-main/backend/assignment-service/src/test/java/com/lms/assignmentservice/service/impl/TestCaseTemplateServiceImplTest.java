package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.service.QuizTemplateResource;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseTemplateServiceImplTest {

    TestCaseTemplateServiceImpl service = new TestCaseTemplateServiceImpl();

    @Test
    void xlsxTemplateCanBeDownloadedAndOpened() throws Exception {
        QuizTemplateResource template = service.getTemplate("xlsx");
        byte[] bytes = StreamUtils.copyToByteArray(template.resource().getInputStream());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(template.filename()).isEqualTo("testcases-template.xlsx");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("input");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("expectedOutput");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue()).isEqualTo("isHidden");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue()).isEqualTo("scoreWeight");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(4).getStringCellValue()).isEqualTo("orderIndex");
        }
    }

    @Test
    void csvTemplateCanBeDownloaded() throws Exception {
        QuizTemplateResource template = service.getTemplate("csv");
        String csv = StreamUtils.copyToString(template.resource().getInputStream(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(template.filename()).isEqualTo("testcases-template.csv");
        assertThat(csv).startsWith("input,expectedOutput,isHidden,scoreWeight,orderIndex");
        assertThat(csv).contains("2,True,0,1,1");
    }
}
