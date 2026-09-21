package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.dto.response.CourseProgressResponse;
import com.lms.courseservice.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses/{courseId}")
@RequiredArgsConstructor
public class LessonProgressController {

    private final LessonProgressService progressService;

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> completeLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody CompleteLessonRequest request) {
        progressService.completeLesson(courseId, lessonId, request);
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu lesson hoàn thành")
                .build();
    }

    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            @PathVariable String courseId) {
        return ApiResponse.<CourseProgressResponse>builder()
                .data(progressService.getCourseProgress(courseId))
                .build();
    }

    @GetMapping("/progress/average")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<Double> getAverageProgress(@PathVariable String courseId) {
        return ApiResponse.<Double>builder()
                .data(progressService.getAverageProgress(courseId))
                .build();
    }
}