package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    // Student chỉ thấy quiz đã published và chưa bị xóa mềm
    Page<Quiz> findByCourseIdAndPublishedTrueAndDeletedFalse(String courseId, Pageable pageable);

    Page<Quiz> findByCourseIdAndLessonIdAndPublishedTrueAndDeletedFalse(String courseId, String lessonId, Pageable pageable);

    // Instructor thấy cả unpublished và deleted (để hiển thị mờ + rollback)
    Page<Quiz> findByCourseId(String courseId, Pageable pageable);

    Page<Quiz> findByCourseIdAndLessonId(String courseId, String lessonId, Pageable pageable);

    // Dùng khi tính gradebook: cần danh sách tất cả quiz của course (chỉ active)
    List<Quiz> findByCourseIdAndPublishedTrueAndDeletedFalse(String courseId);

    // Kiểm tra quiz đã published và chưa bị xóa trước khi cho student bắt đầu làm
    Optional<Quiz> findByQuizIdAndPublishedTrueAndDeletedFalse(Integer quizId);

    boolean existsByQuizIdAndCreatedBy(Integer quizId, String createdBy);

    long countByCourseIdAndPublishedTrueAndDeletedFalse(String courseId);

    @Query("SELECT q.courseId, COUNT(q) FROM Quiz q WHERE q.courseId IN :courseIds AND q.published = true AND q.deleted = false GROUP BY q.courseId")
    List<Object[]> countPublishedGroupByCourseIds(@Param("courseIds") List<String> courseIds);

    @Query("SELECT q.lessonId, COUNT(q) FROM Quiz q WHERE q.courseId = :courseId AND q.published = true AND q.deleted = false AND q.lessonId IS NOT NULL GROUP BY q.lessonId")
    List<Object[]> countPublishedGroupByLessonId(@Param("courseId") String courseId);
}
