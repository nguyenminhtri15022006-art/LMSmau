package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.request.CreateSubmissionRequest;
import com.lms.assignmentservice.dto.request.RunCodeRequest;
import com.lms.assignmentservice.dto.response.RunCodeResponse;
import com.lms.assignmentservice.dto.response.SubmissionResponse;
import com.lms.assignmentservice.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface SubmissionService {
    SubmissionResponse submit(CreateSubmissionRequest request);

    RunCodeResponse runCode(RunCodeRequest request);

    SubmissionResponse getSubmission(String submissionId);

    SubmissionResponse getLatestAccepted(Integer problemId);

    Page<SubmissionResponse> getHistory(Integer problemId, String targetUserId, SubmissionStatus status, Pageable pageable);
}
