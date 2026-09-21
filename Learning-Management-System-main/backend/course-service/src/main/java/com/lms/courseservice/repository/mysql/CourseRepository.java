package com.lms.courseservice.repository.mysql;

import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface CourseRepository extends JpaRepository<Course, String>, JpaSpecificationExecutor<Course> {
    boolean existsByTitle(String title);
    boolean existsByCategoriesId(Long categoryId);
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);
    Page<Course> findByInstructorId(String instructorId, Pageable pageable);
    Page<Course> findByCategoriesId(Long categoryId, Pageable pageable);
    Page<Course> findByStatusAndCategoriesId(CourseStatus status, Long categoryId, Pageable pageable);
    long countByInstructorId(String instructorId);

    @Query("SELECT c.id FROM Course c WHERE c.instructorId = :instructorId")
    List<String> findIdsByInstructorId(@Param("instructorId") String instructorId);
}
