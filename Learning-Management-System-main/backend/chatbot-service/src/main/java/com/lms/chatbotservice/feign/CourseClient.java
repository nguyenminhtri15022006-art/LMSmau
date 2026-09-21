package com.lms.chatbotservice.feign;

import com.lms.chatbotservice.dto.feign.CourseDTO;
import com.lms.chatbotservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "course-service",
        url = "${services.course-service.url}",
        fallback = CourseClient.Fallback.class
)
public interface CourseClient {

    // Lấy danh sách khóa học đã đăng ký của sinh viên — dùng cho UC3
    @GetMapping("/internal/courses/enrolled")
    ApiResponse<List<CourseDTO>> getEnrolledCourses(@RequestParam String userId);

    // Lấy toàn bộ khóa học đang có trên hệ thống — dùng cho UC3
    @GetMapping("/internal/courses")
    ApiResponse<List<CourseDTO>> getAllCourses();

    // Fallback — trả về null khi Course Service không khả dụng
    class Fallback implements CourseClient {

        @Override
        public ApiResponse<List<CourseDTO>> getEnrolledCourses(String userId) {
            return null;
        }

        @Override
        public ApiResponse<List<CourseDTO>> getAllCourses() {
            return null;
        }
    }
}