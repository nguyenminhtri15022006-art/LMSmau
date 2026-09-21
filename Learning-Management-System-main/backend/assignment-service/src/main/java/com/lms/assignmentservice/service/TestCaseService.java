package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.CreateTestCaseRequest;
import com.lms.assignmentservice.dto.request.SyncTestCaseRequest;
import com.lms.assignmentservice.dto.response.BulkTestCaseResponse;
import com.lms.assignmentservice.dto.response.TestCaseResponse;
import com.lms.assignmentservice.entity.TestCase;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TestCaseService {
    TestCaseResponse createTestCase(CreateTestCaseRequest request, Integer problemId);

    BulkTestCaseResponse createBulkTestCases(Integer problemId, List<CreateTestCaseRequest> requestList);

    List<TestCaseResponse> syncTestCases(Integer problemId, List<SyncTestCaseRequest> requestList);

    BulkTestCaseResponse importTestCases(Integer problemId, MultipartFile file);

    List<TestCaseResponse> getTestCasesForApi(Integer problemId);

    List<TestCase> getTestCasesForGradingSystem(Integer problemId);

    TestCaseResponse updateTestCase(CreateTestCaseRequest request, Integer problemId, Long testId);

    void deleteTestCase(Integer problemId, List<Long> testIds);
}
