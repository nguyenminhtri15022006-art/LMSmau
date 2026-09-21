package com.lms.assignmentservice.repository.httpClient;

import com.lms.assignmentservice.dto.response.InternalCourseResponse;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service", fallbackFactory = CourseClient.FallbackFactoryImpl.class,
            url = "${services.course-service.url:http://localhost:8082/course}")
public interface CourseClient {
    /**
     * Kiểm tra course có tồn tại không (dùng khi tạo Problem/Quiz).
     */
    @GetMapping("/internal/courses/{courseId}")
    @CircuitBreaker(name = "courseService")
    InternalCourseResponse getCourse(@PathVariable String courseId);

    /**
     * Kiểm tra user đã enroll course chưa (trước khi cho nộp bài/làm quiz).
     */
    @GetMapping("/internal/courses/{courseId}/enrollments/{userId}")
    @CircuitBreaker(name = "courseService")
    Boolean checkEnrollment(@PathVariable String courseId, @PathVariable String userId);

    @GetMapping("/internal/courses/{courseId}/students/count")
    @CircuitBreaker(name = "courseService")
    Long countStudents(@PathVariable String courseId);

    // ===== Fallback =====

    @Component
    class FallbackFactoryImpl implements FallbackFactory<CourseClient> {
        @Override
        public CourseClient create(Throwable cause) {
            return new CourseClient() {
                @Override
                public InternalCourseResponse getCourse(String courseId) {
                    if (cause instanceof FeignException feignException && feignException.status() == 404) {
                        throw new AssignmentException(ErrorCode.COURSE_NOT_FOUND);
                    }
                    throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }

                @Override
                public Boolean checkEnrollment(String courseId, String userId) {
                    // Fail closed cho permission check khi dịch vụ phụ thuộc bị lỗi.
                    throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }

                @Override
                public Long countStudents(String courseId) {
                    throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }
            };
        }
    }
}
