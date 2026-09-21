package com.lms.blogservice.mapper;

import com.lms.blogservice.dto.request.CreatePostRequest;
import com.lms.blogservice.dto.request.UpdatePostRequest;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PostMapper {

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "thumbnail", ignore = true)
    Post toPost(CreatePostRequest request);

    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "upvoteCount", ignore = true)
    @Mapping(target = "downvoteCount", ignore = true)
    PostResponse toPostResponse(Post post);

    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "upvoteCount", ignore = true)
    PostSummaryResponse toPostSummaryResponse(Post post);

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "thumbnail", ignore = true)
    void updatePost(@MappingTarget Post post, UpdatePostRequest request);
}
