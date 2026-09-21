package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.Question;
import com.lms.assignmentservice.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    // Instructor chỉ thấy câu hỏi của mình
    Page<Question> findByCreatedBy(String createdBy, Pageable pageable);

    Page<Question> findByCreatedByAndTopic(String createdBy, String topic, Pageable pageable);

    Page<Question> findByCreatedByAndType(String createdBy, QuestionType type, Pageable pageable);

    boolean existsByQuestionIdAndCreatedBy(Integer questionId, String createdBy);

    // Lấy toàn bộ ngân hàng câu hỏi của một Course (có phân trang)
    Page<Question> findByCourseId(String courseId, Pageable pageable);

    // Lọc theo topic
    Page<Question> findByCourseIdAndTopic(String courseId, String topic, Pageable pageable);

    // Lọc theo type
    Page<Question> findByCourseIdAndType(String courseId, QuestionType type, Pageable pageable);

    // Lọc kết hợp
    Page<Question> findByCourseIdAndTopicAndType(
            String courseId, String topic, QuestionType type, Pageable pageable);

    // Lấy danh sách topic của ngân hàng (để hiển thị filter)
    @Query("SELECT DISTINCT q.topic FROM Question q WHERE q.courseId = :courseId AND q.topic IS NOT NULL")
    List<String> findDistinctTopicsByCourseId(@Param("courseId") String courseId);

    // Đếm câu hỏi trong ngân hàng (cho phép filter)
    long countByCourseId(String courseId);

    // Kiểm tra câu hỏi thuộc course — dùng để validate quyền
    boolean existsByQuestionIdAndCourseId(Integer questionId, String courseId);

    /**
     * Lôi toàn bộ câu hỏi của một Quiz cùng với danh sách Answers trong 1 query duy nhất.
     * Dùng LEFT JOIN FETCH để avoid N+1 Query Problem trong hàm submitAttempt.
     *
     * @param quizId ID của Quiz
     * @return Danh sách Question với answers đã được eager-load
     */
    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.answers a
            INNER JOIN QuizQuestion qq ON qq.question = q
            WHERE qq.quiz.quizId = :quizId
            """)
    List<Question> findAllByQuizIdWithAnswers(@Param("quizId") Integer quizId);

    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.answers a
            INNER JOIN QuizQuestion qq ON qq.question = q
            WHERE qq.quiz.quizId = :quizId
              AND q.questionId = :questionId
            """)
    Optional<Question> findByQuizIdAndQuestionIdWithAnswers(
            @Param("quizId") Integer quizId,
            @Param("questionId") Integer questionId);
}
