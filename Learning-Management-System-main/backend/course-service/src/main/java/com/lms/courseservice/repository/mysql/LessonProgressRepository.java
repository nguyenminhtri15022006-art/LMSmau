package com.lms.courseservice.repository.mysql;

import com.lms.courseservice.entity.mysql.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByStudentIdAndLessonId(String studentId, String lessonId);

    List<LessonProgress> findByStudentIdAndCourseId(String studentId, String courseId);

    long countByStudentIdAndCourseIdAndCompletedTrue(String studentId, String courseId);

    long countByCourseIdAndCompletedTrue(String courseId);
}