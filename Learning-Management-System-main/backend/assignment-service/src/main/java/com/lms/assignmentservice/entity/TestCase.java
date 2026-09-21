package com.lms.assignmentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    Long testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;

    @Column(name = "input", columnDefinition = "LONGTEXT", nullable = false)
    String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "LONGTEXT")
    String expectedOutput;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    Boolean hidden = true;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    Short orderIndex = 0;

    @Column(name = "score_weight", nullable = false)
    @Builder.Default
    Float scoreWeight = 1.0f;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @PrePersist
    protected void onCreated() {
        createdAt = Instant.now();
    }
}
