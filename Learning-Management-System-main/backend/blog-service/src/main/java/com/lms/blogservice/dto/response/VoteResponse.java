package com.lms.blogservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteResponse {
    Long targetId;
    TargetType targetType;
    VoteType voteType;      // null nếu đã hủy vote
    Long upvoteCount;
    Long downvoteCount;
}