package com.lms.blogservice.mapper;

import com.lms.blogservice.dto.request.UpdateCommentRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CommentMapper {

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "upvoteCount", ignore = true)
    @Mapping(target = "downvoteCount", ignore = true)
    @Mapping(target = "replies", ignore = true)
    CommentResponse toCommentResponse(Comment comment);

    void updateComment(@MappingTarget Comment comment,
                       UpdateCommentRequest request);
}
