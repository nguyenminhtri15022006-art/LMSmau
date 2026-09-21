package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    @Query("SELECT t FROM TestCase t WHERE t.problem.problemId = :problemId ORDER BY t.orderIndex ASC")
    List<TestCase> getTestCasesByProblem(@Param("problemId") Integer problemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestCase t WHERE t.problem.problemId = :problemId AND t.testId IN :testIds")
    int deleteByProblemIdAndTestCaseIds(
            @Param("problemId") Integer problemId,
            @Param("testIds") List<Long> testIds
    );

    // Lấy tất cả test case theo thứ tự để gửi Judge0
    List<TestCase> findByProblem_ProblemIdOrderByOrderIndexAsc(Integer problemId);

    // Chỉ lấy public test case cho STUDENT xem
    List<TestCase> findByProblem_ProblemIdAndHiddenFalseOrderByOrderIndexAsc(Integer problemId);

    void deleteByProblem_ProblemId(Integer problemId);
}
