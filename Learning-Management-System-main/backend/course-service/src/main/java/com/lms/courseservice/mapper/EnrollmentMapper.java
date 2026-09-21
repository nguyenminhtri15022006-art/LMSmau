package com.lms.courseservice.mapper;

import com.lms.courseservice.dto.response.EnrollmentResponse;
import com.lms.courseservice.entity.mysql.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    @Mapping(target = "courseId", source = "course.id")
    EnrollmentResponse toEnrollmentResponse(Enrollment enrollment);
}
