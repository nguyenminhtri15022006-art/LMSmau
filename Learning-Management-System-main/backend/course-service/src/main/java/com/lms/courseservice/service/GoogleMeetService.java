package com.lms.courseservice.service;


import com.lms.courseservice.dto.MeetingInfo;

public interface GoogleMeetService {
    MeetingInfo createMeeting(String title);
    void deleteMeeting(String eventId);
}
