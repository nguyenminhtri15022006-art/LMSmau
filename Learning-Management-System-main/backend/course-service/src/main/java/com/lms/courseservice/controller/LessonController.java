package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.UpdateLessonRequest;
import com.lms.courseservice.dto.response.LessonResponse;
import com.lms.courseservice.service.LessonService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses/{courseId}/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonController {

    LessonService lessonService;

    @GetMapping("/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LessonResponse> getLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        return ApiResponse.<LessonResponse>builder()
                .data(lessonService.getLesson(courseId, lessonId))
                .build();
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestBody @Valid UpdateLessonRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .data(lessonService.updateLesson(courseId, lessonId, request))
                .build();
    }
}
