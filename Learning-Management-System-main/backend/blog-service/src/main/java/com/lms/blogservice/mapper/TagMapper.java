package com.lms.blogservice.mapper;

import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.request.UpdateTagRequest;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TagMapper {
    Tag toTag(CreateTagRequest request);
    TagResponse toTagResponse(Tag tag);
    void updateTag(@MappingTarget Tag tag, UpdateTagRequest request);
}
