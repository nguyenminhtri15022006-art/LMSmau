package com.lms.assignmentservice.entity;

import com.lms.assignmentservice.enums.ShowResultType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    Integer quizId;

    @Column(name = "course_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String courseId;

    @Column(name = "lesson_id", length = 36, columnDefinition = "CHAR(36)")
    String lessonId;

    @Column(nullable = false, length = 300)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    /**
     * Duration in minutes.
     * Nullable: null = "Không giới hạn" (unlimited / no time limit)
     * If present: ranges from 1-300 minutes.
     */
    @Column
    Integer duration;

    @Column(name = "total_score", nullable = false)
    @Builder.Default
    Short totalScore = 100;

    @Column(name = "pass_score", nullable = false)
    @Builder.Default
    Byte passScore = 50;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    Byte maxAttempts = 0;

    @Column(name = "shuffle_questions", nullable = false)
    @Builder.Default
    Boolean shuffleQuestions = true;

    @Column(name = "shuffle_answers", nullable = false)
    @Builder.Default
    Boolean shuffleAnswers = true;

    @Column(name = "show_result")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    ShowResultType showResult = ShowResultType.AFTER_SUBMIT;

    @Column(name = "start_time")
    Instant startTime;

    @Column(name = "end_time")
    Instant endTime;

    @Column(name = "created_by", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    Boolean published = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean deleted = false;

    @Column(name = "deleted_at")
    Instant deletedAt;

    @Column(name = "deleted_by", length = 36, columnDefinition = "CHAR(36)")
    String deletedBy;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isTimeWindowOpen() {
        Instant now = Instant.now();
        if (startTime != null && now.isBefore(startTime)) return false;
        if (endTime != null && now.isAfter(endTime)) return false;
        return true;
    }

    // Helper: tính tổng điểm từ các câu hỏi đã chọn
    public double calculateMaxScore() {
        return quizQuestions.stream()
                .mapToDouble(QuizQuestion::getEffectiveScore)
                .sum();
    }
}
