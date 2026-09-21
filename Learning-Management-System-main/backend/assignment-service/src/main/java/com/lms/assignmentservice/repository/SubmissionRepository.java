package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.document.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends MongoRepository<Submission, String> {
    // Lịch sử nộp bài của student cho một problem (sắp xếp mới nhất trước)
    Page<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(
            String userId,
            Integer problemId,
            Pageable pageable
    );

    Page<Submission> findByUserIdAndProblemIdAndStatusOrderBySubmittedAtDesc(
            String userId,
            Integer problemId,
            String status,
            Pageable pageable
    );

    // Lịch sử nộp bài theo problem (instructor xem)
    Page<Submission> findByProblemIdOrderBySubmittedAtDesc(
            Integer problemId,
            Pageable pageable
    );

    // Tất cả submission PENDING để background job xử lý
    List<Submission> findByStatusOrderBySubmittedAtAsc(String status);

    // Submission mới nhất của student cho problem (dùng hiển thị "code gần nhất")
    Optional<Submission> findTopByUserIdAndProblemIdOrderBySubmittedAtDesc(
            String userId,
            Integer problemId
    );

    Optional<Submission> findTopByUserIdAndProblemIdAndStatusOrderBySubmittedAtDesc(
            String userId,
            Integer problemId,
            String status
    );

    // Kiểm tra problem đã có submission nào chưa (để quyết định soft/hard delete)
    boolean existsByProblemId(Integer problemId);

    // Đếm tổng submission của problem (sync counter cache trong MySQL)
    long countByProblemId(Integer problemId);

    // Đếm số submission AC của problem
    long countByProblemIdAndStatus(Integer problemId, String status);

    // Kiểm tra student đã AC problem này chưa (dùng cho progress tracking)
    boolean existsByUserIdAndProblemIdAndStatus(String userId, Integer problemId, String status);

    // Tất cả submission của student trong một course (dùng tính gradebook)
    @Query("{ 'user_id': ?0, 'course_id': ?1 }")
    List<Submission> findByUserIdAndCourseId(String userId, String courseId);

    @Query(value = "{ 'user_id': ?0, 'course_id': ?1, 'status': ?2 }", fields = "{ 'problem_id': 1 }")
    List<Submission> findCompletionByUserIdAndCourseIdAndStatus(String userId, String courseId, String status);
}
