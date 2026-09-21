package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.request.CreateTestCaseRequest;
import com.lms.assignmentservice.dto.request.SyncTestCaseRequest;
import com.lms.assignmentservice.dto.response.BulkTestCaseResponse;
import com.lms.assignmentservice.dto.response.TestCaseResponse;
import com.lms.assignmentservice.entity.Problem;
import com.lms.assignmentservice.entity.TestCase;
import com.lms.assignmentservice.enums.ErrorTestCaseStatus;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.mapper.TestCaseMapper;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.repository.TestCaseRepository;
import com.lms.assignmentservice.service.CourseResourceAuthorizationService;
import com.lms.assignmentservice.service.TestCaseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TestCaseServiceImpl implements TestCaseService {
    private static final List<String> REQUIRED_IMPORT_HEADERS = List.of(
            "input",
            "expectedoutput",
            "ishidden",
            "scoreweight",
            "orderindex"
    );

    TestCaseRepository testCaseRepository;
    ProblemRepository problemRepository;
    SubmissionRepository submissionRepository;
    TestCaseMapper testCaseMapper;
    CourseResourceAuthorizationService courseResourceAuthorizationService;

    @Value("${assignment.import.test-case-max-file-size-bytes:10485760}")
    @NonFinal
    long maxImportFileSizeBytes;

    @Transactional
    @Override
    public TestCaseResponse createTestCase(CreateTestCaseRequest request, Integer problemId) {
        // kiem tra problem co ton tai
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
        checkNoSubmissions(problemId);

        TestCase testCase = testCaseMapper.toTestCase(request);
        testCase.setProblem(problem);

        testCaseRepository.save(testCase);
        return testCaseMapper.toTestCaseResponse(testCase);
    }

    @Transactional
    @Override
    public BulkTestCaseResponse createBulkTestCases(Integer problemId, List<CreateTestCaseRequest> requestList) {
        // kiem tra problem co ton tai
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));
        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
        checkNoSubmissions(problemId);

        List<TestCase> validTestCases = new ArrayList<>();
        List<BulkTestCaseResponse.TestCaseErrorInfo> errors = new ArrayList<>();

        for (int i = 0; i < requestList.size(); i++) {
            CreateTestCaseRequest request = requestList.get(i);

            //validate testcase
            String validationError = validateTestCase(request);

            if (validationError == null) {
                // neu hop le
                TestCase testCase = testCaseMapper.toTestCase(request);
                testCase.setProblem(problem);
                validTestCases.add(testCase);
            } else {
                // khong hop le
                errors.add(BulkTestCaseResponse.TestCaseErrorInfo.builder()
                        .index(i)
                        .inputData(truncateInput(request.getInput()))
                        .reason(validationError)
                        .build());
            }
        }
        // luu testcase valid vao db
        if (!validTestCases.isEmpty())
            testCaseRepository.saveAll(validTestCases);

        String status = errors.isEmpty()
                ? ErrorTestCaseStatus.SUCCESS.name()
                : (validTestCases.isEmpty()
                   ? ErrorTestCaseStatus.FAILED.name()
                   : ErrorTestCaseStatus.PARTIAL_SUCCESS.name());

        String message = String.format("Đã xử lý %d test cases. Thành công: %d. Thất bại: %d.",
                requestList.size(), validTestCases.size(), errors.size());

        return BulkTestCaseResponse.builder()
                .status(status)
                .message(message)
                .total(requestList.size())
                .successCount(validTestCases.size())
                .failedCount(errors.size())
                .errors(errors)
                .build();

    }

    @Transactional
    @Override
    public List<TestCaseResponse> syncTestCases(Integer problemId, List<SyncTestCaseRequest> requestList) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
        checkNoSubmissions(problemId);

        List<SyncTestCaseRequest> safeRequestList = requestList == null ? List.of() : requestList;
        for (SyncTestCaseRequest request : safeRequestList) {
            if (validateTestCase(request) != null) {
                throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
            }
        }

        List<TestCase> existingTestCases = testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(problemId);
        Map<Long, TestCase> existingById = existingTestCases.stream()
                .collect(Collectors.toMap(TestCase::getTestId, Function.identity()));
        Set<Long> retainedIds = new HashSet<>();
        List<TestCase> upsertedTestCases = new ArrayList<>();

        for (int index = 0; index < safeRequestList.size(); index++) {
            SyncTestCaseRequest request = safeRequestList.get(index);
            TestCase testCase;
            if (request.getTestId() != null) {
                testCase = existingById.get(request.getTestId());
                if (testCase == null) {
                    throw new AssignmentException(ErrorCode.TEST_CASE_NOT_FOUND);
                }
                if (!retainedIds.add(request.getTestId())) {
                    throw new AssignmentException(ErrorCode.VALIDATION_ERROR);
                }
            } else {
                testCase = TestCase.builder()
                        .problem(problem)
                        .build();
            }

            applySyncRequest(testCase, request, (short) index);
            upsertedTestCases.add(testCase);
        }

        List<TestCase> deletedTestCases = existingTestCases.stream()
                .filter(testCase -> !retainedIds.contains(testCase.getTestId()))
                .toList();
        if (!deletedTestCases.isEmpty()) {
            testCaseRepository.deleteAll(deletedTestCases);
        }
        if (!upsertedTestCases.isEmpty()) {
            testCaseRepository.saveAll(upsertedTestCases);
        }

        List<TestCase> syncedTestCases = testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(problemId);
        return testCaseMapper.toListTestCaseResponse(syncedTestCases);
    }

    @Transactional
    @Override
    public BulkTestCaseResponse importTestCases(Integer problemId, MultipartFile file) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));

        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
        checkNoSubmissions(problemId);
        validateImportFile(file);

        ImportedTestCases importedTestCases = parseImportFile(file);
        if (!importedTestCases.errors().isEmpty()) {
            return importFailedResponse(importedTestCases.totalRows(), importedTestCases.errors());
        }
        if (importedTestCases.rows().isEmpty()) {
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors = List.of(importError(
                    0,
                    1,
                    "file",
                    null,
                    "File does not contain any test case rows"
            ));
            return importFailedResponse(0, errors);
        }

        List<TestCase> existingTestCases = testCaseRepository.findByProblem_ProblemIdOrderByOrderIndexAsc(problemId);
        Map<Short, TestCase> existingByOrderIndex = existingTestCases.stream()
                .collect(Collectors.toMap(TestCase::getOrderIndex, Function.identity(), (first, second) -> first));
        Set<Short> retainedOrderIndexes = new HashSet<>();
        List<TestCase> upsertedTestCases = new ArrayList<>();

        for (ParsedTestCaseRow row : importedTestCases.rows()) {
            TestCase testCase = existingByOrderIndex.get(row.orderIndex());
            if (testCase == null) {
                testCase = TestCase.builder()
                        .problem(problem)
                        .build();
            }

            testCase.setInput(row.input());
            testCase.setExpectedOutput(row.expectedOutput());
            testCase.setHidden(row.hidden());
            testCase.setOrderIndex(row.orderIndex());
            testCase.setScoreWeight(row.scoreWeight());
            retainedOrderIndexes.add(row.orderIndex());
            upsertedTestCases.add(testCase);
        }

        List<TestCase> deletedTestCases = existingTestCases.stream()
                .filter(testCase -> !retainedOrderIndexes.contains(testCase.getOrderIndex()))
                .toList();
        if (!deletedTestCases.isEmpty()) {
            testCaseRepository.deleteAll(deletedTestCases);
        }
        testCaseRepository.saveAll(upsertedTestCases);

        int total = importedTestCases.rows().size();
        return BulkTestCaseResponse.builder()
                .status(ErrorTestCaseStatus.SUCCESS.name())
                .message(String.format("Imported %d test cases successfully.", total))
                .total(total)
                .successCount(total)
                .failedCount(0)
                .errors(List.of())
                .build();
    }

    @Override
    public List<TestCaseResponse> getTestCasesForApi(Integer problemId) {
        // validate problemId
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));

        // validate authorize
        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());

        List<TestCase> testCases = testCaseRepository.getTestCasesByProblem(problemId);

        if (testCases == null || testCases.isEmpty()) {
            throw new AssignmentException(ErrorCode.TEST_CASE_NOT_FOUND);
        }

        return testCaseMapper.toListTestCaseResponse(testCases);
    }

    @Override
    public List<TestCase> getTestCasesForGradingSystem(Integer problemId) {
        return testCaseRepository.getTestCasesByProblem(problemId);
    }

    @Transactional
    @Override
    public TestCaseResponse updateTestCase(CreateTestCaseRequest request, Integer problemId, Long testId) {
        // validate problem
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));

        // check authorize
        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
        checkNoSubmissions(problemId);

        //validate testcase
        TestCase testCase = testCaseRepository.findById(testId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.TEST_CASE_NOT_FOUND));
        if (!problemId.equals(testCase.getProblem().getProblemId())) {
            throw new AssignmentException(ErrorCode.TEST_CASE_NOT_FOUND);
        }

        testCaseMapper.updateTestCase(request, testCase);

        testCaseRepository.save(testCase);
        return testCaseMapper.toTestCaseResponse(testCase);
    }

    @Transactional
    @Override
    public void deleteTestCase(Integer problemId, List<Long> testIds) {
        // validate problem
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AssignmentException(ErrorCode.PROBLEM_NOT_FOUND));

        // check authorize
        courseResourceAuthorizationService.checkManager(problem.getCourseId(), problem.getCreatedBy());
        checkNoSubmissions(problemId);

        int count = testCaseRepository.deleteByProblemIdAndTestCaseIds(problemId, testIds);

        if (count == 0)
            throw new AssignmentException(ErrorCode.TEST_CASE_NOT_FOUND);
    }

    // === Helper ===

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AssignmentException(ErrorCode.IMPORT_FILE_EMPTY);
        }
        if (file.getSize() > maxImportFileSizeBytes) {
            throw new AssignmentException(ErrorCode.TEST_CASE_IMPORT_FILE_TOO_LARGE);
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!"csv".equals(extension) && !"xlsx".equals(extension)) {
            throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
        }
    }

    private ImportedTestCases parseImportFile(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        try {
            if ("csv".equals(extension)) {
                return parseCsvImport(file);
            }
            if ("xlsx".equals(extension)) {
                return parseXlsxImport(file);
            }
        } catch (IOException e) {
            log.warn("Cannot read test case import file {}", file.getOriginalFilename(), e);
            throw new AssignmentException(ErrorCode.TEST_CASE_IMPORT_READ_FAILED);
        }
        throw new AssignmentException(ErrorCode.INVALID_IMPORT_FORMAT);
    }

    private ImportedTestCases parseCsvImport(MultipartFile file) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(false)
                .build();

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            Map<String, Integer> headerMap = normalizeHeaders(parser.getHeaderMap());
            List<BulkTestCaseResponse.TestCaseErrorInfo> headerErrors = validateHeaders(headerMap);
            if (!headerErrors.isEmpty()) {
                return new ImportedTestCases(List.of(), headerErrors, 0);
            }

            List<ParsedTestCaseRow> rows = new ArrayList<>();
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors = new ArrayList<>();
            Set<Short> orderIndexes = new HashSet<>();
            int index = 0;
            int totalRows = 0;
            for (CSVRecord record : parser) {
                int rowNumber = Math.toIntExact(record.getRecordNumber()) + 1;
                if (isBlankRecord(record, headerMap)) {
                    continue;
                }
                totalRows++;
                ParsedRowResult rowResult = parseRow(
                        index,
                        rowNumber,
                        getRecordValue(record, headerMap, "input"),
                        getRecordValue(record, headerMap, "expectedoutput"),
                        getRecordValue(record, headerMap, "ishidden"),
                        getRecordValue(record, headerMap, "scoreweight"),
                        getRecordValue(record, headerMap, "orderindex"),
                        orderIndexes
                );
                rows.addAll(rowResult.rows());
                errors.addAll(rowResult.errors());
                index++;
            }
            return new ImportedTestCases(rows, errors, totalRows);
        } catch (IllegalArgumentException e) {
            return new ImportedTestCases(List.of(), List.of(importError(0, 1, "header", null, "Invalid CSV header")), 0);
        }
    }

    private ImportedTestCases parseXlsxImport(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return new ImportedTestCases(List.of(), List.of(importError(0, 1, "file", null, "Workbook has no sheet")), 0);
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> headerMap = normalizeHeaders(headerRow, formatter);
            List<BulkTestCaseResponse.TestCaseErrorInfo> headerErrors = validateHeaders(headerMap);
            if (!headerErrors.isEmpty()) {
                return new ImportedTestCases(List.of(), headerErrors, 0);
            }

            List<ParsedTestCaseRow> rows = new ArrayList<>();
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors = new ArrayList<>();
            Set<Short> orderIndexes = new HashSet<>();
            int index = 0;
            int totalRows = 0;
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, headerMap, formatter)) {
                    continue;
                }
                totalRows++;
                int rowNumber = rowIndex + 1;
                ParsedRowResult rowResult = parseRow(
                        index,
                        rowNumber,
                        getCellValue(row, headerMap, "input", formatter),
                        getCellValue(row, headerMap, "expectedoutput", formatter),
                        getCellValue(row, headerMap, "ishidden", formatter),
                        getCellValue(row, headerMap, "scoreweight", formatter),
                        getCellValue(row, headerMap, "orderindex", formatter),
                        orderIndexes
                );
                rows.addAll(rowResult.rows());
                errors.addAll(rowResult.errors());
                index++;
            }
            return new ImportedTestCases(rows, errors, totalRows);
        } catch (RuntimeException e) {
            log.warn("Cannot parse xlsx test case import file {}", file.getOriginalFilename(), e);
            throw new AssignmentException(ErrorCode.TEST_CASE_IMPORT_READ_FAILED);
        }
    }

    private ParsedRowResult parseRow(
            int index,
            int rowNumber,
            String input,
            String expectedOutput,
            String hiddenValue,
            String scoreWeightValue,
            String orderIndexValue,
            Set<Short> orderIndexes
    ) {
        List<BulkTestCaseResponse.TestCaseErrorInfo> errors = new ArrayList<>();
        String safeInput = input == null ? "" : input;
        if (expectedOutput == null || expectedOutput.trim().isEmpty()) {
            errors.add(importError(index, rowNumber, "expectedOutput", safeInput, "expectedOutput is required"));
        }

        Boolean hidden = parseHidden(hiddenValue, index, rowNumber, safeInput, errors);
        Float scoreWeight = parseScoreWeight(scoreWeightValue, index, rowNumber, safeInput, errors);
        Short orderIndex = parseOrderIndex(orderIndexValue, index, rowNumber, safeInput, errors);
        if (orderIndex != null && !orderIndexes.add(orderIndex)) {
            errors.add(importError(index, rowNumber, "orderIndex", safeInput, "orderIndex must be unique"));
        }

        if (!errors.isEmpty()) {
            return new ParsedRowResult(List.of(), errors);
        }

        return new ParsedRowResult(List.of(new ParsedTestCaseRow(
                rowNumber,
                safeInput,
                expectedOutput,
                hidden,
                scoreWeight,
                orderIndex
        )), List.of());
    }

    private Boolean parseHidden(
            String value,
            int index,
            int rowNumber,
            String input,
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors
    ) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String normalized = value.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        errors.add(importError(index, rowNumber, "isHidden", input, "isHidden must be true or false"));
        return true;
    }

    private Float parseScoreWeight(
            String value,
            int index,
            int rowNumber,
            String input,
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors
    ) {
        if (value == null || value.trim().isEmpty()) {
            return 1.0f;
        }
        try {
            float scoreWeight = Float.parseFloat(value.trim());
            if (scoreWeight <= 0) {
                errors.add(importError(index, rowNumber, "scoreWeight", input, "scoreWeight must be greater than 0"));
            }
            return scoreWeight;
        } catch (NumberFormatException e) {
            errors.add(importError(index, rowNumber, "scoreWeight", input, "scoreWeight must be a number"));
            return 1.0f;
        }
    }

    private Short parseOrderIndex(
            String value,
            int index,
            int rowNumber,
            String input,
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors
    ) {
        if (value == null || value.trim().isEmpty()) {
            return (short) index;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0 || parsed > Short.MAX_VALUE) {
                errors.add(importError(index, rowNumber, "orderIndex", input, "orderIndex must be between 0 and 32767"));
            }
            return (short) parsed;
        } catch (NumberFormatException e) {
            errors.add(importError(index, rowNumber, "orderIndex", input, "orderIndex must be an integer"));
            return (short) index;
        }
    }

    private List<BulkTestCaseResponse.TestCaseErrorInfo> validateHeaders(Map<String, Integer> headerMap) {
        List<BulkTestCaseResponse.TestCaseErrorInfo> errors = new ArrayList<>();
        for (String header : REQUIRED_IMPORT_HEADERS) {
            if (!headerMap.containsKey(header)) {
                errors.add(importError(0, 1, "header", null, "Missing column: " + displayHeader(header)));
            }
        }
        return errors;
    }

    private String displayHeader(String normalizedHeader) {
        return switch (normalizedHeader) {
            case "expectedoutput" -> "expectedOutput";
            case "ishidden" -> "isHidden";
            case "scoreweight" -> "scoreWeight";
            case "orderindex" -> "orderIndex";
            default -> normalizedHeader;
        };
    }

    private Map<String, Integer> normalizeHeaders(Map<String, Integer> headerMap) {
        return headerMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> normalizeHeader(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> left
                ));
    }

    private Map<String, Integer> normalizeHeaders(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            return Map.of();
        }
        List<Cell> cells = new ArrayList<>();
        headerRow.cellIterator().forEachRemaining(cells::add);
        return cells.stream()
                .collect(Collectors.toMap(
                        cell -> normalizeHeader(formatter.formatCellValue(cell)),
                        Cell::getColumnIndex,
                        (left, right) -> left
                ));
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().replace("\uFEFF", "").toLowerCase();
    }

    private String getRecordValue(CSVRecord record, Map<String, Integer> headerMap, String header) {
        Integer columnIndex = headerMap.get(header);
        return columnIndex == null || columnIndex >= record.size() ? null : record.get(columnIndex);
    }

    private String getCellValue(Row row, Map<String, Integer> headerMap, String header, DataFormatter formatter) {
        if (row == null || !headerMap.containsKey(header)) {
            return null;
        }
        Cell cell = row.getCell(headerMap.get(header));
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private boolean isBlankRecord(CSVRecord record, Map<String, Integer> headerMap) {
        return REQUIRED_IMPORT_HEADERS.stream()
                .map(header -> getRecordValue(record, headerMap, header))
                .allMatch(value -> value == null || value.trim().isEmpty());
    }

    private boolean isBlankRow(Row row, Map<String, Integer> headerMap, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        return REQUIRED_IMPORT_HEADERS.stream()
                .map(header -> getCellValue(row, headerMap, header, formatter))
                .allMatch(value -> value == null || value.trim().isEmpty());
    }

    private BulkTestCaseResponse importFailedResponse(
            int totalRows,
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors
    ) {
        return BulkTestCaseResponse.builder()
                .status(ErrorTestCaseStatus.FAILED.name())
                .message("Import failed. Fix all row errors and upload again.")
                .total(totalRows)
                .successCount(0)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    private BulkTestCaseResponse.TestCaseErrorInfo importError(
            int index,
            int rowNumber,
            String field,
            String input,
            String reason
    ) {
        return BulkTestCaseResponse.TestCaseErrorInfo.builder()
                .index(index)
                .rowNumber(rowNumber)
                .field(field)
                .inputData(truncateInput(input))
                .reason(reason)
                .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String validateTestCase(CreateTestCaseRequest request) {
        if (request.getInput() == null) {
            return "Input must not be null";
        }
        if (request.getExpectedOutput() == null || request.getExpectedOutput().trim().isEmpty()) {
            return "Expected Output is required";
        }
        if (request.getScoreWeight() != null && request.getScoreWeight() <= 0) {
            return "Score weight must be greater than 0";
        }
        return null;
    }

    private String validateTestCase(SyncTestCaseRequest request) {
        if (request.getInput() == null) {
            return "Input must not be null";
        }
        if (request.getExpectedOutput() == null || request.getExpectedOutput().trim().isEmpty()) {
            return "Expected Output is required";
        }
        if (request.getScoreWeight() != null && request.getScoreWeight() <= 0) {
            return "Score weight must be greater than 0";
        }
        return null;
    }

    private void applySyncRequest(TestCase testCase, SyncTestCaseRequest request, short defaultOrderIndex) {
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setHidden(request.getHidden() != null ? request.getHidden() : true);
        testCase.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : defaultOrderIndex);
        testCase.setScoreWeight(request.getScoreWeight() != null ? request.getScoreWeight() : 1.0f);
    }

    private void checkNoSubmissions(Integer problemId) {
        if (submissionRepository.existsByProblemId(problemId)) {
            throw new AssignmentException(ErrorCode.PROBLEM_HAS_SUBMISSIONS_CANNOT_MUTATE_TEST_CASES);
        }
    }

    private String truncateInput(String input) {
        if (input == null) return null;
        return input.length() > 50 ? input.substring(0, 50) + "..." : input;
    }

    private record ImportedTestCases(
            List<ParsedTestCaseRow> rows,
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors,
            int totalRows
    ) {
    }

    private record ParsedTestCaseRow(
            int rowNumber,
            String input,
            String expectedOutput,
            Boolean hidden,
            Float scoreWeight,
            Short orderIndex
    ) {
    }

    private record ParsedRowResult(
            List<ParsedTestCaseRow> rows,
            List<BulkTestCaseResponse.TestCaseErrorInfo> errors
    ) {
    }

}
