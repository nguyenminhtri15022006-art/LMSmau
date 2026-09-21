package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.InviteStudentRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.dto.response.CourseStudentResponse;
import com.lms.courseservice.dto.response.EnrollmentResponse;
import com.lms.courseservice.dto.response.UserInfoDto;
import com.lms.courseservice.dto.response.InstructorStatsResponse;
import com.lms.courseservice.dto.response.InviteResultResponse;
import com.lms.courseservice.dto.response.RecentEnrollmentResponse;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.service.CourseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {

    CourseService courseService;

    @GetMapping
    public ApiResponse<Page<CourseResponse>> getAllCourses(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<CourseLevel> level) {
        return ApiResponse.<Page<CourseResponse>>builder()
                .data(courseService.getAllCourses(pageable, status, categoryId, keyword, level))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable String id) {
        return ApiResponse.<CourseResponse>builder()
                .data(courseService.getCourseById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<CourseResponse> createCourse(
            @ModelAttribute @Valid CreateCourseRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .data(courseService.createCourse(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<CourseResponse> updateCourse(
            @PathVariable String id,
            @ModelAttribute @Valid UpdateCourseRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .data(courseService.updateCourse(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<String> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ApiResponse.<String>builder()
                .message("Xóa khóa học thành công")
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<Page<CourseResponse>> getMyCourses(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<CourseResponse>>builder()
                .data(courseService.getMyCourses(pageable))
                .build();
    }

    @GetMapping("/enrolled")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CourseResponse>> getEnrolledCourses() {
        return ApiResponse.<List<CourseResponse>>builder()
                .data(courseService.getEnrolledCourses())
                .build();
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EnrollmentResponse> enrollCourse(@PathVariable String id) {
        return ApiResponse.<EnrollmentResponse>builder()
                .data(courseService.enrollCourse(id))
                .build();
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<InviteResultResponse> inviteStudents(
            @PathVariable String id,
            @RequestBody @Valid InviteStudentRequest request) {
        return ApiResponse.<InviteResultResponse>builder()
                .data(courseService.inviteStudents(id, request))
                .build();
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponse> lockCourse(
            @PathVariable String id,
            @RequestBody @Valid LockCourseRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .data(courseService.lockCourse(id, request))
                .build();
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponse> unlockCourse(@PathVariable String id) {
        return ApiResponse.<CourseResponse>builder()
                .data(courseService.unlockCourse(id))
                .build();
    }

    @GetMapping("/my/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<InstructorStatsResponse> getMyStats() {
        return ApiResponse.<InstructorStatsResponse>builder()
                .data(courseService.getMyStats())
                .build();
    }

    @GetMapping("/my/recent-enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<List<RecentEnrollmentResponse>> getRecentEnrollments(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<List<RecentEnrollmentResponse>>builder()
                .data(courseService.getRecentEnrollments(limit))
                .build();
    }

    @GetMapping("/{id}/students/count")
    public ApiResponse<Long> countStudents(@PathVariable String id) {
        return ApiResponse.<Long>builder()
                .data(courseService.countStudents(id))
                .build();
    }

    @GetMapping("/{id}/lessons/count")
    public ApiResponse<Long> countLessons(@PathVariable String id) {
        return ApiResponse.<Long>builder()
                .data(courseService.countLessons(id))
                .build();
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<Page<CourseStudentResponse>> getCourseStudents(
            @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<CourseStudentResponse>>builder()
                .data(courseService.getCourseStudents(id, pageable))
                .build();
    }

    @DeleteMapping("/{id}/students/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<String> removeStudent(
            @PathVariable String id,
            @PathVariable String userId) {
        courseService.removeStudentFromCourse(id, userId);
        return ApiResponse.<String>builder().message("Xóa học viên khỏi khóa học thành công").build();
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<UserInfoDto> searchUserByEmail(@RequestParam String email) {
        return ApiResponse.<UserInfoDto>builder()
                .data(courseService.searchUserByEmail(email))
                .build();
    }
}
