package com.lms.courseservice.mapper;

import com.lms.courseservice.dto.request.UpdateScheduleRequest;
import com.lms.courseservice.dto.response.ScheduleResponse;
import com.lms.courseservice.entity.mysql.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ScheduleMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "meetingUrl", source = "course.meetingUrl")
    ScheduleResponse toScheduleResponse(Schedule schedule);

    void updateSchedule(@MappingTarget Schedule schedule,
                        UpdateScheduleRequest request);
}
