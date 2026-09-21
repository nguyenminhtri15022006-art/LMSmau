package com.lms.courseservice.repository.mongo;

import com.lms.courseservice.entity.mongo.CourseStructure;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CourseStructureRepository extends MongoRepository<CourseStructure,String> {
    Optional<CourseStructure> findByCourseId(String courseId);
}
