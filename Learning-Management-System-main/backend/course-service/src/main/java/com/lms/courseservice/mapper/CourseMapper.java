package com.lms.courseservice.mapper;

import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.entity.mysql.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CourseMapper {

    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "mongoStructureId", ignore = true)
    @Mapping(target = "instructorId", ignore = true)
    @Mapping(target = "meetingUrl", ignore = true)
    @Mapping(target = "googleEventId", ignore = true)
    Course toCourse(CreateCourseRequest request);

    @Mapping(target = "exerciseCount", ignore = true)
    CourseResponse toCourseResponse(Course course);

    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "mongoStructureId", ignore = true)
    @Mapping(target = "instructorId", ignore = true)
    @Mapping(target = "meetingUrl", ignore = true)
    @Mapping(target = "googleEventId", ignore = true)
    void updateCourse(@MappingTarget Course course, UpdateCourseRequest request);
}