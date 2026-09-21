package com.lms.assignmentservice.entity;

import com.lms.assignmentservice.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ngân hàng câu hỏi — KHÔNG có FK trực tiếp vào quizzes.
 * Liên kết qua bảng quiz_questions để 1 câu hỏi dùng được nhiều quiz.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    Integer questionId;

    // Cross-service reference — KHÔNG @ManyToOne vật lý
    @Column(name = "course_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String courseId;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    QuestionType type;

    @Column(length = 100)
    String topic;

    @Column(columnDefinition = "TEXT")
    String explanation;

    @Column(nullable = false)
    @Builder.Default
    Byte score = 10;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Column(name = "created_by", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Answer> answers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
