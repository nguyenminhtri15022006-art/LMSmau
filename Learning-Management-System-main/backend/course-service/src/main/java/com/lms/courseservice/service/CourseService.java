package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.InviteStudentRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.dto.response.CourseStudentResponse;
import com.lms.courseservice.dto.response.EnrollmentResponse;
import com.lms.courseservice.dto.response.UserInfoDto;
import com.lms.courseservice.dto.response.InstructorStatsResponse;
import com.lms.courseservice.dto.response.InviteResultResponse;
import com.lms.courseservice.dto.response.RecentEnrollmentResponse;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {
    Page<CourseResponse> getAllCourses(Pageable pageable, CourseStatus status, Long categoryId, String keyword, List<CourseLevel> levels);
    CourseResponse getCourseById(String id);
    CourseResponse createCourse(CreateCourseRequest request);
    CourseResponse updateCourse(String id, UpdateCourseRequest request);
    void deleteCourse(String id);
    EnrollmentResponse enrollCourse(String courseId);
    InviteResultResponse inviteStudents(String courseId, InviteStudentRequest request);
    List<CourseResponse> getEnrolledCourses();
    Page<CourseResponse> getMyCourses(Pageable pageable);
    CourseResponse lockCourse(String courseId, LockCourseRequest request);
    CourseResponse unlockCourse(String courseId);
    long countStudents(String courseId);
    long countLessons(String courseId);
    InstructorStatsResponse getMyStats();
    List<RecentEnrollmentResponse> getRecentEnrollments(int limit);
    Page<CourseStudentResponse> getCourseStudents(String courseId, Pageable pageable);
    void removeStudentFromCourse(String courseId, String userId);
    UserInfoDto searchUserByEmail(String email);
}
