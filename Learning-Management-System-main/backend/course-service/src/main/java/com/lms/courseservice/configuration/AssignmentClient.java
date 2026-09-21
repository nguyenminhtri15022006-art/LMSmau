package com.lms.courseservice.configuration;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "assignment-service",
        url = "${services.assignment-service.url}",
        fallback = AssignmentClient.Fallback.class
)
public interface AssignmentClient {

    @GetMapping("/internal/courses/{courseId}/exercise-count")
    ApiResponse<Long> getExerciseCount(@PathVariable String courseId);

    @PostMapping("/internal/courses/exercise-counts")
    ApiResponse<Map<String, Long>> getExerciseCountBatch(@RequestBody List<String> courseIds);

    @Component
    class Fallback implements AssignmentClient {
        @Override
        public ApiResponse<Long> getExerciseCount(String courseId) {
            return ApiResponse.<Long>builder().data(null).build();
        }

        @Override
        public ApiResponse<Map<String, Long>> getExerciseCountBatch(List<String> courseIds) {
            return ApiResponse.<Map<String, Long>>builder().data(Map.of()).build();
        }
    }
}