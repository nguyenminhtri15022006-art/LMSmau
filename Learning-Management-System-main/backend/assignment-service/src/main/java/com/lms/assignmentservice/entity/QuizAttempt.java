package com.lms.assignmentservice.entity;

import com.lms.assignmentservice.enums.AttemptStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder

public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    Quiz quiz;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String userId;

    @Column(name = "score", nullable = false)
    @Builder.Default
    Float score = 0f;

    @Column(name = "total_score", nullable = false)
    BigDecimal totalScore;

    @Column(name = "is_passed", nullable = false)
    @Builder.Default
    Boolean passed = false;

    @Column(name = "time_spent_s")
    Integer timeSpentS;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    AttemptStatusType status = AttemptStatusType.IN_PROGRESS;

    @Column(name = "started_at", nullable = false, updatable = false)
    Instant startedAt;

    @Column(name = "submitted_at")
    Instant submittedAt;

    @Column(name = "expires_at")
    Instant expiresAt;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    Byte attemptNumber = 1;

    /**
     * Version field cho Optimistic Locking.
     * <p>
     * Kịch bản: Học viên auto-save ở giây 00:30, tích tắc bấm Submit.
     * 2 luồng cùng cố ghi vào bảng quiz_answer_records.
     * <p>
     * Giải pháp:
     * 1. Submit luồng chạy trước, tăng version (1 → 2)
     * 2. Auto-save luồng chạy sau cố cập nhật với version=1 (cũ)
     * 3. Hibernate phát hiện version mismatch, ném OptimisticLockException
     * 4. Auto-save catch lỗi và log, bỏ qua (Submit đã chứa data mới nhất rồi)
     */
    @Version
    @Column(name = "version")
    @Builder.Default
    Long version = 0L;

    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<QuizAnswerRecord> answerRecords = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
    }

    private static final long GRACE_PERIOD_SECONDS = 15;

    /**
     * Kiểm tra hết hạn với grace period 15 giây để bù trừ clock skew và độ trễ mạng.
     * Dùng cho: submitAttempt, autoSaveAttempt, startOrResumeAttempt.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt.plusSeconds(GRACE_PERIOD_SECONDS));
    }

    /**
     * Kiểm tra hết hạn nghiêm ngặt (không grace period).
     * Dùng cho: cleanup job, quyết định trạng thái cuối cùng.
     */
    public boolean isStrictlyExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
