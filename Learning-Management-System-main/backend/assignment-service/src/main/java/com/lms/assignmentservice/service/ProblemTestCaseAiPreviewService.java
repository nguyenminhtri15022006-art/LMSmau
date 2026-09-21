package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.AiTestCasePreviewRequest;
import com.lms.assignmentservice.dto.response.AiTestCasePreviewResponse;

public interface ProblemTestCaseAiPreviewService {
    AiTestCasePreviewResponse preview(AiTestCasePreviewRequest request);
}
