package com.lms.courseservice.service.impl;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.lms.courseservice.configuration.GoogleCalendarConfig;
import com.lms.courseservice.dto.MeetingInfo;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.service.GoogleMeetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMeetServiceImpl implements GoogleMeetService {

    private final GoogleCalendarConfig googleCalendarConfig;

    @Value("${google.calendar.calendar-id}")
    private String calendarId;

    @Value("${google.calendar.meet-event-duration-days:1}")
    private long meetEventDurationDays;

    @Value("${google.calendar.conference-type:hangoutsMeet}")
    private String conferenceType;

    @Value("${google.calendar.send-updates:none}")
    private String sendUpdates;

    @Override
    public MeetingInfo createMeeting(String title) {
        try {
            Calendar calendar = googleCalendarConfig.buildCalendarClient();

            String today = LocalDate.now().toString();
            String tomorrow = LocalDate.now().plusDays(meetEventDurationDays).toString();

            Event event = new Event()
                    .setSummary(title)
                    .setStart(new EventDateTime().setDate(new DateTime(today)))
                    .setEnd(new EventDateTime().setDate(new DateTime(tomorrow)));

            event.setConferenceData(new ConferenceData()
                    .setCreateRequest(new CreateConferenceRequest()
                            .setRequestId(UUID.randomUUID().toString())
                            .setConferenceSolutionKey(new ConferenceSolutionKey().setType(conferenceType))));

            Event createdEvent = calendar.events()
                    .insert(calendarId, event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates(sendUpdates)
                    .execute();

            return MeetingInfo.builder()
                    .meetingUrl(createdEvent.getHangoutLink())
                    .eventId(createdEvent.getId())
                    .build();

        } catch (IOException | GeneralSecurityException e) {
            log.warn("Google Calendar API lỗi khi tạo meeting [{}], dùng link giả: {}", title, e.getMessage());
            return MeetingInfo.builder()
                    .meetingUrl(generateFallbackMeetUrl())
                    .eventId(null)
                    .build();
        }
    }

    private String generateFallbackMeetUrl() {
        String uid = UUID.randomUUID().toString().replace("-", "");
        return "https://meet.google.com/lms-" + uid.substring(0, 4) + "-" + uid.substring(4, 8);
    }

    @Override
    public void deleteMeeting(String eventId) {
        try {
            Calendar calendar = googleCalendarConfig.buildCalendarClient();
            calendar.events().delete(calendarId, eventId).execute();

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) return;
            throw new CourseException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);

        } catch (IOException | GeneralSecurityException e) {
            log.error("Google Calendar API lỗi khi xóa meeting [{}]: {}", eventId, e.getMessage(), e);
            throw new CourseException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }
}
