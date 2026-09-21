package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizQuizIdOrderByOrderIndexAsc(Integer quizId);

    boolean existsByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    // Câu hỏi này đang được dùng trong bao nhiêu quiz?
    long countByQuestionQuestionId(Integer questionId);

    @Query("""
            SELECT COUNT(qq)
            FROM QuizQuestion qq
            WHERE qq.question.questionId = :questionId
              AND (
                    qq.quiz.published = true
                    OR EXISTS (
                        SELECT a
                        FROM QuizAttempt a
                        WHERE a.quiz.quizId = qq.quiz.quizId
                    )
              )
            """)
    long countLockedQuizReferencesByQuestionId(@Param("questionId") Integer questionId);

    void deleteByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);
}
