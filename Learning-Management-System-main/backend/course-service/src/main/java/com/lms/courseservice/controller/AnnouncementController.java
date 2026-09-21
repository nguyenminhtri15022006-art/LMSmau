package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.CreateAnnouncementRequest;
import com.lms.courseservice.dto.response.AnnouncementResponse;
import com.lms.courseservice.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses/{courseId}/announcements")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnnouncementController {

    AnnouncementService announcementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<AnnouncementResponse>> getAnnouncements(
            @PathVariable String courseId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.<Page<AnnouncementResponse>>builder()
                .data(announcementService.getAnnouncements(courseId, pageable))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<AnnouncementResponse> createAnnouncement(
            @PathVariable String courseId,
            @RequestBody @Valid CreateAnnouncementRequest request) {
        return ApiResponse.<AnnouncementResponse>builder()
                .data(announcementService.createAnnouncement(courseId, request))
                .build();
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<String> deleteAnnouncement(
            @PathVariable String courseId,
            @PathVariable Long announcementId) {
        announcementService.deleteAnnouncement(courseId, announcementId);
        return ApiResponse.<String>builder()
                .message("Xóa thông báo thành công")
                .build();
    }
}
