package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.entity.mysql.LessonProgress;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.repository.mysql.LessonProgressRepository;
import com.lms.courseservice.service.CourseStructureService;
import com.lms.courseservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceImplTest {

    @Mock
    LessonProgressRepository progressRepo;
    @Mock
    EnrollmentRepository enrollmentRepo;
    @Mock
    CourseStructureService structureService;

    @InjectMocks
    LessonProgressServiceImpl lessonProgressService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void completeLessonRejectsStudentWhoIsNotEnrolled() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        when(enrollmentRepo.existsByUserIdAndCourseId("student-1", "course-1")).thenReturn(false);

        assertThatThrownBy(() -> lessonProgressService.completeLesson(
                "course-1",
                "lesson-1",
                request(LessonType.VIDEO)))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_ENROLLED);
    }

    @Test
    void completeLessonCreatesCompletedProgressRecord() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        when(enrollmentRepo.existsByUserIdAndCourseId("student-1", "course-1")).thenReturn(true);
        when(structureService.isLessonInCourse("course-1", "lesson-1")).thenReturn(true);
        when(progressRepo.findByStudentIdAndLessonId("student-1", "lesson-1")).thenReturn(Optional.empty());

        lessonProgressService.completeLesson("course-1", "lesson-1", request(LessonType.VIDEO));

        ArgumentCaptor<LessonProgress> captor = ArgumentCaptor.forClass(LessonProgress.class);
        verify(progressRepo).save(captor.capture());
        assertThat(captor.getValue().getStudentId()).isEqualTo("student-1");
        assertThat(captor.getValue().getCourseId()).isEqualTo("course-1");
        assertThat(captor.getValue().getLessonId()).isEqualTo("lesson-1");
        assertThat(captor.getValue().getLessonType()).isEqualTo(LessonType.VIDEO);
        assertThat(captor.getValue().isCompleted()).isTrue();
        assertThat(captor.getValue().getLastAccessed()).isNotNull();
    }

    @Test
    void completeLessonRejectsLessonOutsideCourse() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        when(enrollmentRepo.existsByUserIdAndCourseId("student-1", "course-1")).thenReturn(true);
        when(structureService.isLessonInCourse("course-1", "lesson-1")).thenReturn(false);

        assertThatThrownBy(() -> lessonProgressService.completeLesson(
                "course-1",
                "lesson-1",
                request(LessonType.DOCUMENT)))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LESSON_NOT_BELONG_TO_COURSE);
    }

    @Test
    void getCourseProgressCalculatesPercentAndCompletedLessonIds() {
        TestSecurity.authenticate("student-1", "ROLE_STUDENT");
        when(structureService.countLessons("course-1")).thenReturn(4);
        when(progressRepo.findByStudentIdAndCourseId("student-1", "course-1")).thenReturn(List.of(
                LessonProgress.builder().lessonId("lesson-1").completed(true).build(),
                LessonProgress.builder().lessonId("lesson-2").completed(false).build(),
                LessonProgress.builder().lessonId("lesson-3").completed(true).build()));

        var progress = lessonProgressService.getCourseProgress("course-1");

        assertThat(progress.getTotalLessons()).isEqualTo(4);
        assertThat(progress.getCompletedLessons()).isEqualTo(2);
        assertThat(progress.getPercentComplete()).isEqualTo(50.0);
        assertThat(progress.getCompletedLessonIds()).containsExactly("lesson-1", "lesson-3");
    }

    @Test
    void getAverageProgressReturnsZeroWithoutLessonsOrStudentsAndCapsAtOneHundred() {
        when(structureService.countLessons("empty-course")).thenReturn(0);
        assertThat(lessonProgressService.getAverageProgress("empty-course")).isZero();

        when(structureService.countLessons("course-1")).thenReturn(2);
        when(enrollmentRepo.countByCourseId("course-1")).thenReturn(1L);
        when(progressRepo.countByCourseIdAndCompletedTrue("course-1")).thenReturn(3L);

        assertThat(lessonProgressService.getAverageProgress("course-1")).isEqualTo(100.0);
    }

    private CompleteLessonRequest request(LessonType lessonType) {
        CompleteLessonRequest request = new CompleteLessonRequest();
        request.setLessonType(lessonType);
        return request;
    }
}
