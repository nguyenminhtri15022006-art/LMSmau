package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.AssignmentClient;
import com.lms.courseservice.configuration.IdentityClient;
import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.MeetingInfo;
import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.InviteStudentRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.dto.response.EnrollmentResponse;
import com.lms.courseservice.dto.response.InviteResultResponse;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.EnrollmentStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CourseMapper;
import com.lms.courseservice.mapper.EnrollmentMapper;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.AnnouncementRepository;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.GoogleMeetService;
import com.lms.courseservice.service.MinioService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    EnrollmentRepository enrollmentRepository;
    @Mock
    AnnouncementRepository announcementRepository;
    @Mock
    CourseStructureRepository courseStructureRepository;
    @Mock
    LessonRepository lessonRepository;
    @Mock
    CourseMapper courseMapper;
    @Mock
    EnrollmentMapper enrollmentMapper;
    @Mock
    MinioService minioService;
    @Mock
    IdentityClient identityClient;
    @Mock
    AssignmentClient assignmentClient;
    @Mock
    GoogleMeetService googleMeetService;

    @InjectMocks
    CourseServiceImpl courseService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createCourseCreatesMongoStructureAndUsesCurrentInstructor() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        CreateCourseRequest request = CreateCourseRequest.builder()
                .title("Spring Boot")
                .status(CourseStatus.PUBLIC)
                .build();
        Course mapped = Course.builder().title("Spring Boot").status(CourseStatus.PUBLIC).build();
        CourseResponse response = CourseResponse.builder().id("course-1").title("Spring Boot").build();

        when(courseRepository.existsByTitle("Spring Boot")).thenReturn(false);
        when(courseMapper.toCourse(request)).thenReturn(mapped);
        when(googleMeetService.createMeeting("Spring Boot"))
                .thenReturn(MeetingInfo.builder().meetingUrl("https://meet").eventId("event-1").build());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            if (course.getId() == null) {
                course.setId("course-1");
            }
            return course;
        });
        when(courseStructureRepository.save(any(CourseStructure.class))).thenAnswer(invocation -> {
            CourseStructure structure = invocation.getArgument(0);
            structure.setId("structure-1");
            return structure;
        });
        when(courseMapper.toCourseResponse(mapped)).thenReturn(response);

        CourseResponse actual = courseService.createCourse(request);

        assertThat(actual.getId()).isEqualTo("course-1");
        assertThat(mapped.getInstructorId()).isEqualTo("instructor-1");
        assertThat(mapped.getMeetingUrl()).isEqualTo("https://meet");
        assertThat(mapped.getGoogleEventId()).isEqualTo("event-1");
        assertThat(mapped.getMongoStructureId()).isEqualTo("structure-1");
        verify(courseStructureRepository).save(any(CourseStructure.class));
    }

    @Test
    void enrollCourseCreatesActiveEnrollmentForPublicCourse() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Course course = Course.builder().id("course-1").status(CourseStatus.PUBLIC).build();
        EnrollmentResponse response = EnrollmentResponse.builder()
                .courseId("course-1")
                .userId("student-1")
                .status(EnrollmentStatus.ACTIVE)
                .build();

        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId("student-1", "course-1")).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentMapper.toEnrollmentResponse(any(Enrollment.class))).thenReturn(response);

        EnrollmentResponse actual = courseService.enrollCourse("course-1");

        assertThat(actual.getUserId()).isEqualTo("student-1");
        assertThat(actual.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        assertThat(enrollmentCaptor.getValue().getCourse()).isSameAs(course);
    }

    @Test
    void enrollCourseRejectsLockedCourseBeforeCheckingDuplicateEnrollment() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        Course course = Course.builder().id("course-1").status(CourseStatus.LOCKED).build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.enrollCourse("course-1"))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_LOCKED);

        verify(enrollmentRepository, never()).existsByUserIdAndCourseId("student-1", "course-1");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void lockCourseSetsStatusAndCreatesSystemAnnouncement() {
        Course course = Course.builder().id("course-1").status(CourseStatus.PUBLIC).build();
        CourseResponse response = CourseResponse.builder().id("course-1").status(CourseStatus.LOCKED).build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        when(courseMapper.toCourseResponse(course)).thenReturn(response);

        CourseResponse actual = courseService.lockCourse(
                "course-1",
                LockCourseRequest.builder().reason("policy violation").build());

        assertThat(actual.getStatus()).isEqualTo(CourseStatus.LOCKED);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.LOCKED);
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        assertThat(captor.getValue().getCourse()).isSameAs(course);
        assertThat(captor.getValue().getContent()).contains("policy violation");
    }

    @Test
    void inviteStudentsSeparatesSuccessAlreadyEnrolledAndMissingUsers() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(identityClient.existsById("new-student")).thenReturn(true);
        when(identityClient.existsById("already-student")).thenReturn(true);
        when(identityClient.existsById("missing-student")).thenReturn(false);
        when(enrollmentRepository.existsByUserIdAndCourseId(any(), eq("course-1")))
                .thenAnswer(invocation -> "already-student".equals(invocation.getArgument(0)));

        InviteResultResponse result = courseService.inviteStudents(
                "course-1",
                InviteStudentRequest.builder()
                        .userIds(List.of("new-student", "already-student", "missing-student"))
                        .build());

        assertThat(result.getSuccessIds()).containsExactly("new-student");
        assertThat(result.getAlreadyIds()).containsExactly("already-student");
        assertThat(result.getNotFoundIds()).containsExactly("missing-student");
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void getMyStatsFallsBackToZeroExercisesWhenAssignmentServiceFails() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        when(courseRepository.countByInstructorId("instructor-1")).thenReturn(2L);
        when(enrollmentRepository.countTotalStudentsByInstructorId("instructor-1")).thenReturn(5L);
        when(courseRepository.findIdsByInstructorId("instructor-1")).thenReturn(List.of("course-1", "course-2"));
        when(assignmentClient.getExerciseCountBatch(anyList())).thenThrow(new RuntimeException("down"));

        var stats = courseService.getMyStats();

        assertThat(stats.getTotalCourses()).isEqualTo(2);
        assertThat(stats.getTotalStudents()).isEqualTo(5);
        assertThat(stats.getTotalExercises()).isZero();
    }

    @Test
    void getAllCoursesPopulatesExerciseCountsWhenAssignmentServiceResponds() {
        Course course = Course.builder().id("course-1").title("Java").build();
        CourseResponse response = CourseResponse.builder().id("course-1").title("Java").build();
        when(courseRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Course>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(course)));
        when(courseMapper.toCourseResponse(course)).thenReturn(response);
        when(assignmentClient.getExerciseCountBatch(List.of("course-1")))
                .thenReturn(ApiResponse.<Map<String, Long>>builder()
                        .data(Map.of("course-1", 3L))
                        .build());

        var page = courseService.getAllCourses(PageRequest.of(0, 10), null, null, null, null);

        assertThat(page.getContent()).singleElement()
                .extracting(CourseResponse::getExerciseCount)
                .isEqualTo(3L);
    }
}
