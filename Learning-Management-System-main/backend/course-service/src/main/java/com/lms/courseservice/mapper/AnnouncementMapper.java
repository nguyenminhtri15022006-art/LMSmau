package com.lms.courseservice.mapper;

import com.lms.courseservice.dto.response.AnnouncementResponse;
import com.lms.courseservice.entity.mysql.Announcement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AnnouncementMapper {

    @Mapping(target = "courseId", source = "course.id")
    AnnouncementResponse toAnnouncementResponse(Announcement announcement);
}
