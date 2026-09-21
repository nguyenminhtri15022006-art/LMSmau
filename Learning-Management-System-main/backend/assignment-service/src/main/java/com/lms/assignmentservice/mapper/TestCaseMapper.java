package com.lms.assignmentservice.mapper;

import com.lms.assignmentservice.dto.request.CreateTestCaseRequest;
import com.lms.assignmentservice.dto.response.TestCaseResponse;
import com.lms.assignmentservice.entity.TestCase;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TestCaseMapper {
    TestCase toTestCase(CreateTestCaseRequest request);

    TestCaseResponse toTestCaseResponse(TestCase testCase);

    void updateTestCase(CreateTestCaseRequest request, @MappingTarget TestCase testCase);

    List<TestCaseResponse> toListTestCaseResponse(List<TestCase> testCases);
}
