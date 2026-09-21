package com.lms.assignmentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder

/**
 * Bảng join giữa Quiz và Question.
 *
 * Cho phép:
 * - Tái sử dụng câu hỏi ở nhiều quiz khác nhau
 * - Override điểm cho từng quiz cụ thể (null = dùng score gốc của Question)
 * - Sắp xếp thứ tự câu hỏi trong quiz
 */
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    Question question;

    @Column(name = "override_score")
    Short overrideScore;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    Short orderIndex = 0;

    /**
     * Trả về điểm thực tế của câu hỏi trong quiz này.
     * Nếu có override thì dùng override, ngược lại dùng score gốc.
     */
    public short getEffectiveScore() {
        return overrideScore != null ? overrideScore : question.getScore();
    }
}
