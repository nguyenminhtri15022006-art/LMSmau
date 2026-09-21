package com.lms.courseservice.repository.mysql;

import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class CourseSpecifications {

    private CourseSpecifications() {}

    public static Specification<Course> hasStatus(CourseStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Course> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();
            query.distinct(true);
            return cb.equal(root.join("categories", JoinType.LEFT).get("id"), categoryId);
        };
    }

    public static Specification<Course> hasLevelIn(List<CourseLevel> levels) {
        return (root, query, cb) ->
                (levels == null || levels.isEmpty()) ? cb.conjunction() : root.get("level").in(levels);
    }

    public static Specification<Course> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
