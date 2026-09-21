package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.service.QuestionFileReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionFileReaderImpl implements QuestionFileReader {

    @Override
    public String readTxt(MultipartFile file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot read TXT question file", e);
        }
        return sb.toString();
    }

    @Override
    public String readDocx(MultipartFile file) {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text.trim()).append("\n");
                } else {
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot read DOCX question file", e);
        }
        return sb.toString();
    }

    @Override
    public List<CSVRecord> readCsvRecords(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            return new ArrayList<>(parser.getRecords());
        } catch (Exception e) {
            throw new RuntimeException("Cannot read CSV question file", e);
        }
    }
}
