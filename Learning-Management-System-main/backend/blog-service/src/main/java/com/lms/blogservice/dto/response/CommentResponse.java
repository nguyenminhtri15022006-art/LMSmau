package com.lms.blogservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class CommentResponse {
    Long id;
    Long postId;
    String userId;
    Long parentId;
    String content;
    Long upvoteCount;
    Long downvoteCount;
    List<CommentResponse> replies;  // comment lồng nhau
    Instant createdAt;
    Instant updatedAt;
}
