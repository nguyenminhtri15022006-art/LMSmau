package com.lms.courseservice.mapper;

import com.lms.courseservice.dto.response.LessonResponse;
import com.lms.courseservice.entity.mongo.Lesson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LessonMapper {
    LessonResponse toLessonResponse(Lesson lesson);
}