package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.CreateScheduleRequest;
import com.lms.courseservice.dto.response.ScheduleResponse;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Schedule;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.ScheduleMapper;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.repository.mysql.ScheduleRepository;
import com.lms.courseservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    ScheduleRepository scheduleRepository;
    @Mock
    CourseRepository courseRepository;
    @Mock
    EnrollmentRepository enrollmentRepository;
    @Mock
    ScheduleMapper scheduleMapper;

    @InjectMocks
    ScheduleServiceImpl scheduleService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void getSchedulesAllowsEnrolledStudent() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Course course = Course.builder().id("course-1").instructorId("instructor-1").build();
        Schedule schedule = Schedule.builder().id(1L).course(course).title("Live class").build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId("student-1", "course-1")).thenReturn(true);
        when(scheduleRepository.findByCourseId("course-1")).thenReturn(List.of(schedule));
        when(scheduleMapper.toScheduleResponse(schedule)).thenReturn(ScheduleResponse.builder().id(1L).title("Live class").build());

        var result = scheduleService.getSchedules("course-1");

        assertThat(result).singleElement()
                .extracting(ScheduleResponse::getTitle)
                .isEqualTo("Live class");
    }

    @Test
    void getSchedulesRejectsOutsiderStudent() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Course course = Course.builder().id("course-1").instructorId("instructor-1").build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId("student-1", "course-1")).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.getSchedules("course-1"))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void createScheduleRejectsLockedCourse() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.LOCKED)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> scheduleService.createSchedule("course-1", createRequest()))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_LOCKED);
    }

    @Test
    void createSchedulePersistsScheduleForOwner() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleMapper.toScheduleResponse(any(Schedule.class))).thenReturn(ScheduleResponse.builder().title("Live class").build());

        scheduleService.createSchedule("course-1", createRequest());

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getCourse()).isSameAs(course);
        assertThat(captor.getValue().getTitle()).isEqualTo("Live class");
    }

    private CreateScheduleRequest createRequest() {
        return CreateScheduleRequest.builder()
                .title("Live class")
                .startTime(Instant.now().plusSeconds(86_400))
                .endTime(Instant.now().plusSeconds(93_600))
                .note("Zoom")
                .build();
    }
}
