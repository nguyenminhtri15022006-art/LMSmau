package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.QuizAttempt;
import com.lms.assignmentservice.enums.AttemptStatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    // Kiểm tra student đang có attempt IN_PROGRESS không
    Optional<QuizAttempt> findByQuiz_QuizIdAndUserIdAndStatus(
            Integer quizId, String userId, AttemptStatusType status);

    // Lịch sử attempt theo thời gian giảm dần
    Page<QuizAttempt> findByUserIdAndQuiz_QuizIdOrderByStartedAtDesc(
            String userId, Integer quizId, Pageable pageable);

    // Đếm số lần đã làm để kiểm tra max_attempts
    long countByUserIdAndQuiz_QuizId(String userId, Integer quizId);

    // SELECT 1 FROM quiz_attempts WHERE quiz_id = ? LIMIT 1
    boolean existsByQuiz_QuizId(Integer quizId);

    @Query("""
            SELECT a.quiz.quizId, COUNT(a)
            FROM QuizAttempt a
            WHERE a.quiz.quizId IN :quizIds
            GROUP BY a.quiz.quizId
            """)
    List<Object[]> countGroupByQuizIds(@Param("quizIds") List<Integer> quizIds);

    // Best score — dùng tính gradebook on-the-fly
    @Query("""
            SELECT MAX(a.score)
            FROM QuizAttempt a
            WHERE a.userId = :userId
              AND a.quiz.quizId = :quizId
              AND a.status = 'SUBMITTED'
            """)
    Optional<BigDecimal> findBestScore(
            @Param("userId") String userId,
            @Param("quizId") Integer quizId);

    // Tổng hợp best score nhiều quiz một lúc — dùng trong gradebook
    @Query("""
            SELECT a.quiz.quizId, MAX(a.score)
            FROM QuizAttempt a
            WHERE a.userId = :userId
              AND a.quiz.quizId IN :quizIds
              AND a.status = 'SUBMITTED'
            GROUP BY a.quiz.quizId
            """)
    List<Object[]> findBestScorePerQuiz(
            @Param("userId") String userId,
            @Param("quizIds") List<Integer> quizIds);

    // Tổng thời gian làm bài trung bình mỗi user trong course (phút)
    @Query("""
            SELECT a.userId, AVG(a.timeSpentS)
            FROM QuizAttempt a
            WHERE a.quiz.courseId = :courseId
              AND a.status = 'SUBMITTED'
              AND a.timeSpentS IS NOT NULL
            GROUP BY a.userId
            """)
    List<Object[]> aggregateAvgTimeByCourse(@Param("courseId") String courseId);

    @Query("""
            SELECT DISTINCT a.quiz.quizId
            FROM QuizAttempt a
            WHERE a.userId = :userId
              AND a.quiz.courseId = :courseId
              AND a.status = 'SUBMITTED'
              AND a.passed = true
            """)
    List<Integer> findPassedQuizIdsByUserIdAndCourseId(
            @Param("userId") String userId,
            @Param("courseId") String courseId);

    @Query("""
            SELECT COUNT(a)
            FROM QuizAttempt a
            WHERE a.quiz.courseId = :courseId
              AND a.status = 'SUBMITTED'
            """)
    long countSubmittedByCourseId(@Param("courseId") String courseId);

    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT qa.user_id, qa.quiz_id
                FROM quiz_attempts qa
                JOIN quizzes q ON q.quiz_id = qa.quiz_id
                WHERE q.course_id = :courseId
                  AND qa.status = 'SUBMITTED'
                GROUP BY qa.user_id, qa.quiz_id
            ) completed_pairs
            """, nativeQuery = true)
    long countUniqueSubmittedStudentQuizPairs(@Param("courseId") String courseId);

    @Query(value = """
            SELECT DATE(qa.submitted_at) AS activity_date, COUNT(*) AS submission_count
            FROM quiz_attempts qa
            JOIN quizzes q ON q.quiz_id = qa.quiz_id
            WHERE q.course_id = :courseId
              AND qa.status = 'SUBMITTED'
              AND qa.submitted_at >= :start
              AND qa.submitted_at < :end
            GROUP BY DATE(qa.submitted_at)
            ORDER BY activity_date
            """, nativeQuery = true)
    List<Object[]> countSubmittedActivityByCourse(
            @Param("courseId") String courseId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query(value = """
            SELECT a
            FROM QuizAttempt a
            JOIN FETCH a.quiz q
            WHERE q.courseId = :courseId
              AND a.status = 'SUBMITTED'
            ORDER BY a.submittedAt DESC
            """,
            countQuery = """
            SELECT COUNT(a)
            FROM QuizAttempt a
            WHERE a.quiz.courseId = :courseId
              AND a.status = 'SUBMITTED'
            """)
    Page<QuizAttempt> findSubmittedByCourseIdOrderBySubmittedAtDesc(
            @Param("courseId") String courseId,
            Pageable pageable);

    // ===== Background Job: Abandoned Session Cleanup =====

    /**
     * Tìm attempts đã hết hạn (expiresAt < now) và chưa được xử lý.
     * Dùng cho Job dọn dẹp attempts quá hạn mỗi 1 phút.
     * Sử dụng Pageable để tránh OOM khi có hàng nghìn attempt.
     */
    @Query("""
            SELECT a FROM QuizAttempt a
            WHERE a.status = 'IN_PROGRESS'
              AND a.expiresAt IS NOT NULL
              AND a.expiresAt < :now
            """)
    Page<QuizAttempt> findByStatusAndExpiresAtBefore(
            @Param("now") Instant now, Pageable pageable);

    /**
     * Tìm attempts bỏ hoang (không có expiresAt hoặc expiresAt chưa đến, nhưng startedAt quá lâu).
     * Dùng cho Job dọn dẹp attempts bỏ hoang mỗi 1 giờ.
     * Sử dụng Pageable để tránh OOM khi có hàng nghìn attempt.
     */
    @Query("""
            SELECT a FROM QuizAttempt a
            WHERE a.status = 'IN_PROGRESS'
              AND a.startedAt < :threshold
            """)
    Page<QuizAttempt> findByStatusAndStartedAtBefore(
            @Param("threshold") Instant threshold, Pageable pageable);
}
