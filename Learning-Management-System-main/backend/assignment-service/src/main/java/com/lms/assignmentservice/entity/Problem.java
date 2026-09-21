package com.lms.assignmentservice.entity;

import com.lms.assignmentservice.enums.DifficultyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "problem_id")
    Integer problemId;

    @Column(name = "course_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String courseId;

    @Column(name = "lesson_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String lessonId;

    @Column(name = "title", length = 300)
    String title;

    @Column(name = "slug", length = 350, unique = true)
    String slug;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    DifficultyType difficulty;

    @Column(name = "time_limit_ms", nullable = false)
    @Builder.Default
    Integer timeLimitMs = 2000;

    @Column(name = "memory_limit_mb", nullable = false)
    @Builder.Default
    Integer memoryLimitMb = 256;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_langs", nullable = false, columnDefinition = "JSON")
    @Builder.Default
    List<String> allowedLangs = new ArrayList<>();

    @Column(name = "score", nullable = false)
    @Builder.Default
    Short score = 100;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    Boolean isPublic = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean deleted = false;

    @Column(name = "deleted_at")
    Instant deletedAt;

    @Column(name = "deleted_by", length = 36, columnDefinition = "CHAR(36)")
    String deletedBy;

    @Column(name = "total_submit", nullable = false)
    @Builder.Default
    Integer totalSubmit = 0;

    @Column(name = "total_accepted", nullable = false)
    @Builder.Default
    Integer totalAccepted = 0;

    @Column(name = "created_by", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    String createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TestCase> testCases = new ArrayList<>();

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
