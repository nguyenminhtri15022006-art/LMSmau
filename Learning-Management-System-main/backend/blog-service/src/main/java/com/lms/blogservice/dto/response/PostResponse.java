package com.lms.blogservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.blogservice.enums.PostStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostResponse {
    Long id;
    String title;
    String slug;
    String summary;
    String content;
    String thumbnail;
    String authorId;
    PostStatus status;
    Long viewCount;
    Long commentCount;
    Long upvoteCount;
    Long downvoteCount;
    List<TagResponse> tags;
    Instant createdAt;
    Instant updatedAt;
}
