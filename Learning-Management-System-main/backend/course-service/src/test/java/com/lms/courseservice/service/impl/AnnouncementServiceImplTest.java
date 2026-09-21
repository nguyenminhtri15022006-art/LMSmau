package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.CreateAnnouncementRequest;
import com.lms.courseservice.dto.response.AnnouncementResponse;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.AnnouncementMapper;
import com.lms.courseservice.repository.mysql.AnnouncementRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceImplTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    AnnouncementRepository announcementRepository;
    @Mock
    EnrollmentRepository enrollmentRepository;
    @Mock
    AnnouncementMapper announcementMapper;

    @InjectMocks
    AnnouncementServiceImpl announcementService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void getAnnouncementsAllowsCourseInstructor() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder().id("course-1").instructorId("instructor-1").build();
        Announcement announcement = Announcement.builder().id(1L).title("Exam").content("Friday").course(course).build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(announcementRepository.findByCourseId("course-1", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(announcement)));
        when(announcementMapper.toAnnouncementResponse(announcement))
                .thenReturn(AnnouncementResponse.builder().id(1L).title("Exam").build());

        var page = announcementService.getAnnouncements("course-1", PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement()
                .extracting(AnnouncementResponse::getTitle)
                .isEqualTo("Exam");
    }

    @Test
    void createAnnouncementRejectsLockedCourse() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.LOCKED)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> announcementService.createAnnouncement("course-1", request()))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_LOCKED);
    }

    @Test
    void createAnnouncementPersistsForCourseOwner() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(announcementMapper.toAnnouncementResponse(any(Announcement.class)))
                .thenReturn(AnnouncementResponse.builder().title("Exam").build());

        announcementService.createAnnouncement("course-1", request());

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        assertThat(captor.getValue().getCourse()).isSameAs(course);
        assertThat(captor.getValue().getTitle()).isEqualTo("Exam");
        assertThat(captor.getValue().getContent()).isEqualTo("Friday");
    }

    @Test
    void deleteAnnouncementRejectsMissingAnnouncement() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.deleteAnnouncement("course-1", 99L))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
    }

    private CreateAnnouncementRequest request() {
        return CreateAnnouncementRequest.builder()
                .title("Exam")
                .content("Friday")
                .build();
    }
}
