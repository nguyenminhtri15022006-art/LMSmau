package com.lms.assignmentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Đáp án của câu hỏi.
 * <p>
 * CRITICAL: field `correct` TUYỆT ĐỐI không được map ra DTO
 * khi trả response cho student đang làm bài.
 * Chỉ expose sau khi attempt đã SUBMITTED.
 */
@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    Integer answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    Question question;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    Boolean correct = false;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    Short orderIndex = 0;
}