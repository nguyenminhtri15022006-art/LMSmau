package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.CreateScheduleRequest;
import com.lms.courseservice.dto.request.UpdateScheduleRequest;
import com.lms.courseservice.dto.response.ScheduleResponse;
import com.lms.courseservice.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/schedules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduleController {

    ScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ScheduleResponse>> getSchedules(
            @PathVariable String courseId) {
        return ApiResponse.<List<ScheduleResponse>>builder()
                .data(scheduleService.getSchedules(courseId))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<ScheduleResponse> createSchedule(
            @PathVariable String courseId,
            @RequestBody @Valid CreateScheduleRequest request) {
        return ApiResponse.<ScheduleResponse>builder()
                .data(scheduleService.createSchedule(courseId, request))
                .build();
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @PathVariable String courseId,
            @PathVariable Long scheduleId,
            @RequestBody @Valid UpdateScheduleRequest request) {
        return ApiResponse.<ScheduleResponse>builder()
                .data(scheduleService.updateSchedule(courseId, scheduleId, request))
                .build();
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<String> deleteSchedule(
            @PathVariable String courseId,
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(courseId, scheduleId);
        return ApiResponse.<String>builder()
                .message("Xóa lịch học thành công")
                .build();
    }
}
