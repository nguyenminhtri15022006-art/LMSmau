package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.service.QuestionFileContentExtractor;
import com.lms.assignmentservice.service.QuestionFileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionFileContentExtractorImpl implements QuestionFileContentExtractor {

    private final QuestionFileReader questionFileReader;

    @Override
    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AssignmentException(ErrorCode.IMPORT_FILE_EMPTY);
        }

        String extension = getExtension(file.getOriginalFilename());
        try {
            return switch (extension) {
                case "txt" -> questionFileReader.readTxt(file);
                case "docx" -> questionFileReader.readDocx(file);
                case "csv" -> extractCsvContext(file);
                default -> throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
            };
        } catch (AssignmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error extracting AI quiz source file: {}", e.getMessage(), e);
            throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
        }
    }

    private String extractCsvContext(MultipartFile file) {
        StringBuilder sb = new StringBuilder();
        for (CSVRecord record : questionFileReader.readCsvRecords(file)) {
            for (int i = 0; i < record.size(); i++) {
                String value = record.get(i);
                if (value != null && !value.isBlank()) {
                    sb.append(value.trim()).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
