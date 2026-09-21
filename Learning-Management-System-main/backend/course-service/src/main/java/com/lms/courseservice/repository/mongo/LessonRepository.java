package com.lms.courseservice.repository.mongo;

import com.lms.courseservice.entity.mongo.Lesson;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LessonRepository extends MongoRepository<Lesson,String> {
    List<Lesson> findByCourseId(String courseId);
    boolean existsByCourseIdAndId(String courseId, String lessonId);
    void deleteAllByCourseId(String courseId);
    long countByCourseId(String courseId);
}
