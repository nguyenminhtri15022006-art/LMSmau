package com.lms.blogservice.entity;

import com.lms.blogservice.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 500)
    String title;

    @Column(nullable = false, unique = true, length = 500)
    String slug;

    @Column(length = 1000)
    String summary;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    String content;

    @Column(length = 1000)
    String thumbnail;

    @Column(name = "author_id", nullable = false, length = 36)
    String authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PostStatus status;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    Long viewCount = 0L;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Comment> comments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    List<Tag> tags = new ArrayList<>();

    @Column(name = "created_at", updatable = false, nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
        if (status == null) status = PostStatus.DRAFT;
        if (viewCount == null) viewCount = 0L;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
