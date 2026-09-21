package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.request.GenerateQuizRequest;
import com.lms.chatbotservice.dto.request.GenerateTestCaseRequest;
import com.lms.chatbotservice.dto.response.GenerateQuizResponse;
import com.lms.chatbotservice.dto.response.GenerateTestCaseResponse;

public interface GenerateService {

    // UC4a — Tạo quiz
    GenerateQuizResponse generateQuiz(GenerateQuizRequest request);

    // UC4b — Tạo test case
    GenerateTestCaseResponse generateTestCase(GenerateTestCaseRequest request);
}