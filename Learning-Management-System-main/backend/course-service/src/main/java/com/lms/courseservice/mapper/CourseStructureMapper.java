package com.lms.courseservice.mapper;

import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.StructureNodeResponse;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mongo.StructureNode;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseStructureMapper {
    StructureNodeResponse toNodeResponse(StructureNode node);
    CourseStructureResponse toCourseStructureResponse(CourseStructure structure);
}
