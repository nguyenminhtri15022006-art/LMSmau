package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.AdminCodeJudgeActivityResponse;
import com.lms.assignmentservice.service.AdminCodeJudgeActivityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/code-judge")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCodeJudgeActivityController {

    AdminCodeJudgeActivityService adminCodeJudgeActivityService;

    @GetMapping("/activity")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<AdminCodeJudgeActivityResponse>> getActivity(
            @RequestParam(defaultValue = "24") int hours) {
        return ApiResponse.<List<AdminCodeJudgeActivityResponse>>builder()
                .data(adminCodeJudgeActivityService.getActivity(hours))
                .build();
    }
}
