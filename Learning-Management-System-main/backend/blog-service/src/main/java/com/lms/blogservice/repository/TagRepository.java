package com.lms.blogservice.repository;

import com.lms.blogservice.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    Optional<Tag> findBySlug(String slug);
    List<Tag> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT COUNT(p) > 0 FROM Post p JOIN p.tags t WHERE t.id = :tagId")
    boolean isTagUsedByAnyPost(@Param("tagId") Long tagId);
}
