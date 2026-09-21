package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.CreateScheduleRequest;
import com.lms.courseservice.dto.request.UpdateScheduleRequest;
import com.lms.courseservice.dto.response.ScheduleResponse;

import java.util.List;

public interface ScheduleService {
    List<ScheduleResponse> getSchedules(String courseId);
    ScheduleResponse createSchedule(String courseId, CreateScheduleRequest request);
    ScheduleResponse updateSchedule(String courseId, Long scheduleId,
                                    UpdateScheduleRequest request);
    void deleteSchedule(String courseId, Long scheduleId);
}
