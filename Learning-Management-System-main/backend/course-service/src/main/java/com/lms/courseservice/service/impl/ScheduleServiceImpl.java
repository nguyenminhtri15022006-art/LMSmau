package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.CreateScheduleRequest;
import com.lms.courseservice.dto.request.UpdateScheduleRequest;
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
import com.lms.courseservice.service.ScheduleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduleServiceImpl implements ScheduleService {

    ScheduleRepository scheduleRepository;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleResponse> getSchedules(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkAccessPermission(course);
        return scheduleRepository.findByCourseId(courseId)
                .stream()
                .map(scheduleMapper::toScheduleResponse)
                .toList();
    }

    @Override
    public ScheduleResponse createSchedule(String courseId, CreateScheduleRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);
        validateNewScheduleTime(request.getStartTime(), request.getEndTime());

        Schedule schedule = Schedule.builder()
                .course(course)
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .note(request.getNote())
                .build();

        return scheduleMapper.toScheduleResponse(
                scheduleRepository.save(schedule));
    }

    @Override
    public ScheduleResponse updateSchedule(String courseId, Long scheduleId, UpdateScheduleRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);
        validateScheduleOrder(request.getStartTime(), request.getEndTime());

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CourseException(ErrorCode.SCHEDULE_NOT_FOUND));

        scheduleMapper.updateSchedule(schedule, request);
        return scheduleMapper.toScheduleResponse(
                scheduleRepository.save(schedule));
    }

    @Override
    public void deleteSchedule(String courseId, Long scheduleId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CourseException(ErrorCode.SCHEDULE_NOT_FOUND));

        scheduleRepository.delete(schedule);
    }

    // ============ HELPER =============

    private void checkNotLocked(Course course) {
        if (CourseStatus.LOCKED.equals(course.getStatus()))
            throw new CourseException(ErrorCode.COURSE_LOCKED);
    }

    private void validateNewScheduleTime(Instant startTime, Instant endTime) {
        validateScheduleOrder(startTime, endTime);
        if (startTime.isBefore(Instant.now())) {
            throw new CourseException(ErrorCode.SCHEDULE_TIME_INVALID);
        }
    }

    private void validateScheduleOrder(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new CourseException(ErrorCode.SCHEDULE_TIME_INVALID);
        }
    }

    private void checkCourseOwner(Course course){

        String userId = GatewayAuthentication.currentUserId();
        String role = GatewayAuthentication.currentRole();

        if("ROLE_ADMIN".equals(role)) return;
        if(!course.getInstructorId().equals(userId))
            throw new CourseException(ErrorCode.NOT_COURSE_OWNER);
    }

    private void checkAccessPermission(Course course){
        String userId = GatewayAuthentication.currentUserId();
        String role = GatewayAuthentication.currentRole();

        if("ROLE_ADMIN".equals(role)
            || ("ROLE_INSTRUCTOR".equals(role) && course.getInstructorId().equals(userId))) return;
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId()))
            throw new CourseException(ErrorCode.ACCESS_DENIED);
    }

}
