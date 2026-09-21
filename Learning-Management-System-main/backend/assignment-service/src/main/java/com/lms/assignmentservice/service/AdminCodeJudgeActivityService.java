package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.AdminCodeJudgeActivityResponse;

import java.util.List;

public interface AdminCodeJudgeActivityService {
    List<AdminCodeJudgeActivityResponse> getActivity(int hours);
}
