package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.response.ChatbotCourseResponse;
import com.lms.courseservice.dto.response.InternalCourseResponse;
import com.lms.courseservice.dto.response.InternalLessonResponse;
import com.lms.courseservice.service.InternalCourseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalCourseController {

    InternalCourseService internalCourseService;

    // Lấy thông tin khóa học — Assignment Service gọi để validate
    @GetMapping("/courses/{courseId}")
    public InternalCourseResponse getCourse(
            @PathVariable String courseId) {
        return internalCourseService.getCourse(courseId);
    }

    // Lấy thông tin bài học — Assignment Service gọi để validate
    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public InternalLessonResponse getLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        return internalCourseService.getLesson(courseId, lessonId);
    }

    // Kiểm tra học viên đã enroll chưa — Assignment Service gọi để kiểm tra quyền nộp bài
    @GetMapping("/courses/{courseId}/enrollments/{userId}")
    public Boolean checkEnrollment(
            @PathVariable String courseId,
            @PathVariable String userId) {
        return internalCourseService.existsEnrollment(courseId, userId);
    }

    // Kiểm tra course có tồn tại không
    @GetMapping("/courses/{courseId}/students/count")
    public Long countStudents(
            @PathVariable String courseId) {
        return internalCourseService.countStudents(courseId);
    }

    @GetMapping("/courses/{courseId}/exists")
    public Boolean checkCourse(
            @PathVariable String courseId) {
        return internalCourseService.existsCourse(courseId);
    }

    // Kiểm tra lesson có tồn tại và thuộc course không
    @GetMapping("/courses/{courseId}/lessons/{lessonId}/exists")
    public Boolean checkLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        return internalCourseService.existsLesson(courseId, lessonId);
    }

    // Chatbot UC3 — Lấy toàn bộ khóa học PUBLIC
    @GetMapping("/courses")
    public ApiResponse<List<ChatbotCourseResponse>> getAllPublishedCourses() {
        return ApiResponse.<List<ChatbotCourseResponse>>builder()
                .data(internalCourseService.getAllPublishedCourses())
                .build();
    }

    // Chatbot UC3 — Lấy danh sách khóa học đã enroll của user
    @GetMapping("/courses/enrolled")
    public ApiResponse<List<ChatbotCourseResponse>> getEnrolledCourses(
            @RequestParam String userId) {
        return ApiResponse.<List<ChatbotCourseResponse>>builder()
                .data(internalCourseService.getEnrolledCourses(userId))
                .build();
    }
}
