package com.lms.assignmentservice.service;

import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionFileReader {
    String readTxt(MultipartFile file);

    String readDocx(MultipartFile file);

    List<CSVRecord> readCsvRecords(MultipartFile file);
}
