package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.CreateAnnouncementRequest;
import com.lms.courseservice.dto.response.AnnouncementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {
    Page<AnnouncementResponse> getAnnouncements(String courseId, Pageable pageable);
    AnnouncementResponse createAnnouncement(String courseId,
                                            CreateAnnouncementRequest request);
    void deleteAnnouncement(String courseId, Long announcementId);
}