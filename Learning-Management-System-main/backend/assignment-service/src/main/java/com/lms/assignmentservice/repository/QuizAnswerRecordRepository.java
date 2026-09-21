package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.QuizAnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho QuizAnswerRecord.
 * Lưu các câu trả lời của học viên trong từng lần làm bài.
 */
public interface QuizAnswerRecordRepository extends JpaRepository<QuizAnswerRecord, Long> {

    /**
     * Tìm tất cả records của một attempt (để load lại bài làm hoặc tính điểm).
     */
    List<QuizAnswerRecord> findByQuizAttempt_AttemptId(Long attemptId);

    /**
     * Xóa tất cả records của một attempt (khi auto-save bulk).
     */
    void deleteByQuizAttempt_AttemptId(Long attemptId);

    /**
     * Tìm record của question trong attempt (nếu cần check unique constraint trước upsert).
     */
    @Query("""
            SELECT r FROM QuizAnswerRecord r
            WHERE r.quizAttempt.attemptId = :attemptId
              AND r.question.questionId = :questionId
            """)
    Optional<QuizAnswerRecord> findByAttemptAndQuestion(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Integer questionId);

    @Modifying
    @Query(value = """
            INSERT INTO quiz_answer_records (attempt_id, question_id, selected_answer_ids, earned_score)
            VALUES (:attemptId, :questionId, :selectedAnswerIds, :earnedScore)
            ON DUPLICATE KEY UPDATE
                selected_answer_ids = VALUES(selected_answer_ids),
                earned_score = VALUES(earned_score)
            """, nativeQuery = true)
    int upsertRecord(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Integer questionId,
            @Param("selectedAnswerIds") String selectedAnswerIds,
            @Param("earnedScore") BigDecimal earnedScore);
}

