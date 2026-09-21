package com.lms.blogservice.entity;

import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "votes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "target_id", "target_type"}
        ))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    VoteType voteType;

    @Column(name = "created_at", updatable = false, nullable = false)
    Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
