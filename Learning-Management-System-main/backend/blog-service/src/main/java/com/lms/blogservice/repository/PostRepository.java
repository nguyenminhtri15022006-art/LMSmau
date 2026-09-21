package com.lms.blogservice.repository;

import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    boolean existsByTagsId(Long id);
    boolean existsBySlug(String slug);
    boolean existsByTitle(String title);
    Optional<Post> findBySlug(String slug);
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    Page<Post> findByAuthorId(String authorId, Pageable pageable);
    Page<Post> findByAuthorIdAndStatus(String authorId,
                                       PostStatus status, Pageable pageable);
    Page<Post> findByTagsId(Long tagId, Pageable pageable);
    Page<Post> findByStatusAndTagsId(PostStatus status,
                                     Long tagId, Pageable pageable);
    Page<Post> findByTitleContainingIgnoreCaseAndStatus(String keyword,
                                                        PostStatus status,
                                                        Pageable pageable);
    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Post> findByTitleContainingIgnoreCaseAndTagsId(String keyword,
                                                        Long tagId,
                                                        Pageable pageable);
}
