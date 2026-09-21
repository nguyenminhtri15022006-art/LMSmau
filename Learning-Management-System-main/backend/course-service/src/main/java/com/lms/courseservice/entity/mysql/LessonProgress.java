package com.lms.courseservice.entity.mysql;

import com.lms.courseservice.enums.LessonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String studentId;

    @Column(nullable = false, length = 36)
    private String courseId;

    @Column(nullable = false, length = 36)
    private String lessonId;

    @Enumerated(EnumType.STRING)  // lưu "VIDEO"/"DOCUMENT"
    @Column(nullable = false)
    private LessonType lessonType;

    @Column(name = "is_completed")
    private boolean completed;

    private LocalDateTime lastAccessed;
}