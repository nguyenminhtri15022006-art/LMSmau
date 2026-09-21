package com.lms.courseservice.service.impl;

import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalCourseServiceImplTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    LessonRepository lessonRepository;
    @Mock
    EnrollmentRepository enrollmentRepository;

    @InjectMocks
    InternalCourseServiceImpl internalCourseService;

    @Test
    void getCourseReturnsInternalCourseContract() {
        Course course = Course.builder()
                .id("course-1")
                .title("Java")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        var response = internalCourseService.getCourse("course-1");

        assertThat(response.getId()).isEqualTo("course-1");
        assertThat(response.getInstructorId()).isEqualTo("instructor-1");
        assertThat(response.getStatus()).isEqualTo(CourseStatus.PUBLIC);
    }

    @Test
    void getLessonRejectsLessonFromAnotherCourse() {
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(Course.builder().id("course-1").build()));
        when(lessonRepository.findById("lesson-1"))
                .thenReturn(Optional.of(Lesson.builder().id("lesson-1").courseId("course-2").build()));

        assertThatThrownBy(() -> internalCourseService.getLesson("course-1", "lesson-1"))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LESSON_NOT_BELONG_TO_COURSE);
    }

    @Test
    void getLessonReturnsInternalLessonContract() {
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(Course.builder().id("course-1").build()));
        when(lessonRepository.findById("lesson-1"))
                .thenReturn(Optional.of(Lesson.builder()
                        .id("lesson-1")
                        .courseId("course-1")
                        .lessonType(LessonType.VIDEO)
                        .build()));

        var response = internalCourseService.getLesson("course-1", "lesson-1");

        assertThat(response.getId()).isEqualTo("lesson-1");
        assertThat(response.getCourseId()).isEqualTo("course-1");
        assertThat(response.getLessonType()).isEqualTo(LessonType.VIDEO);
    }

    @Test
    void getAllPublishedCoursesMapsChatbotFields() {
        Course course = Course.builder()
                .id("course-1")
                .title("Java")
                .description("Core Java")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .level(CourseLevel.BEGINNER)
                .duration(12)
                .categories(List.of(Category.builder().name("Backend").build()))
                .learningPoints(List.of("OOP"))
                .requirements(List.of("Basic syntax"))
                .build();
        when(courseRepository.findByStatus(CourseStatus.PUBLIC, Pageable.unpaged()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(course)));

        var courses = internalCourseService.getAllPublishedCourses();

        assertThat(courses).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo("course-1");
            assertThat(response.getStatus()).isEqualTo("PUBLIC");
            assertThat(response.getCategoryNames()).containsExactly("Backend");
            assertThat(response.getLevel()).isEqualTo("BEGINNER");
        });
    }

    @Test
    void getEnrolledCoursesMapsEnrollmentCourses() {
        Course course = Course.builder()
                .id("course-1")
                .title("Java")
                .status(CourseStatus.PUBLIC)
                .categories(List.of())
                .build();
        when(enrollmentRepository.findByUserId("student-1"))
                .thenReturn(List.of(Enrollment.builder().course(course).build()));

        var courses = internalCourseService.getEnrolledCourses("student-1");

        assertThat(courses).singleElement()
                .extracting("id")
                .isEqualTo("course-1");
    }
}
