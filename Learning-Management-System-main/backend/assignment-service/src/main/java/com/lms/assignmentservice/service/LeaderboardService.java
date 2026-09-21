package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.LeaderboardEntryResponse;

import java.util.List;

public interface LeaderboardService {

    List<LeaderboardEntryResponse> getCodingLeaderboard(String courseId);

    List<LeaderboardEntryResponse> getQuizLeaderboard(String courseId);
}
