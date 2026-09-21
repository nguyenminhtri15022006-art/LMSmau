package com.lms.courseservice.entity.mysql;

import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "courses")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Course {

    @Id
    @Column(length = 36)
    String id;

    @Column(nullable = false, length = 500)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "instructor_id", nullable = false, length = 36)
    String instructorId;

    @Column(name = "thumbnail_url", nullable = true, length = 1000)
    String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CourseStatus status;

    @Column(nullable = true)
    Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    CourseLevel level;

    @Column(name = "meeting_url", nullable = true,  length = 500)
    String meetingUrl;

    @Column(name = "google_event_id", length = 200)
    String googleEventId;

    @Column(name = "mongo_structure_id", nullable = false,  length = 36, unique = true)
    String mongoStructureId;

    @ManyToMany
    @JoinTable(
            name = "course_categories",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    List<Category> categories = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_learning_points",
            joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "point", columnDefinition = "TEXT")
    @OrderColumn(name = "point_order")
    List<String> learningPoints = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_requirements",
            joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "requirement", columnDefinition = "TEXT")
    @OrderColumn(name = "requirement_order")
    List<String> requirements = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = CourseStatus.PRIVATE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
