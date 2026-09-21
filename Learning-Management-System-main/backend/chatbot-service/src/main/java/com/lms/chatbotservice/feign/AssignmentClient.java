package com.lms.chatbotservice.feign;

import com.lms.chatbotservice.dto.feign.ProblemDTO;
import com.lms.chatbotservice.dto.feign.SubmissionDTO;
import com.lms.chatbotservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "assignment-service",
        url = "${services.assignment-service.url}",
        fallback = AssignmentClient.Fallback.class
)
public interface AssignmentClient {

    // Lấy đề bài theo problem_id — dùng cho UC2
    @GetMapping("/internal/problems/{problemId}")
    ApiResponse<ProblemDTO> getProblem(@PathVariable Integer problemId);

    // Lấy lần nộp bài gần nhất của sinh viên — dùng cho UC2
    @GetMapping("/internal/submissions/latest")
    ApiResponse<SubmissionDTO> getLatestSubmission(
            @RequestParam Integer problemId,
            @RequestParam String userId
    );

    // Fallback — trả về null khi Assignment Service không khả dụng
    class Fallback implements AssignmentClient {

        @Override
        public ApiResponse<ProblemDTO> getProblem(Integer problemId) {
            return null;
        }

        @Override
        public ApiResponse<SubmissionDTO> getLatestSubmission(
                Integer problemId, String userId) {
            return null;
        }
    }
}