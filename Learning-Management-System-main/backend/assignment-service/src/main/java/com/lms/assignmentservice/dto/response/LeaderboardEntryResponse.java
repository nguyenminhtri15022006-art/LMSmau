package com.lms.assignmentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardEntryResponse {
    String userId;
    String name;
    String avatarUrl;
    Integer score;
    Integer solvedCount;
    Integer completedCount;
    Double accuracy;
    Integer avgTime;
    Boolean currentUser;
}
