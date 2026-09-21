package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.LeaderboardEntryResponse;
import com.lms.assignmentservice.service.LeaderboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LeaderboardController {

    LeaderboardService leaderboardService;

    @GetMapping("/{courseId}/leaderboard/coding")
    public ApiResponse<List<LeaderboardEntryResponse>> getCodingLeaderboard(@PathVariable String courseId) {
        return ApiResponse.<List<LeaderboardEntryResponse>>builder()
                .data(leaderboardService.getCodingLeaderboard(courseId))
                .build();
    }

    @GetMapping("/{courseId}/leaderboard/quiz")
    public ApiResponse<List<LeaderboardEntryResponse>> getQuizLeaderboard(@PathVariable String courseId) {
        return ApiResponse.<List<LeaderboardEntryResponse>>builder()
                .data(leaderboardService.getQuizLeaderboard(courseId))
                .build();
    }
}
