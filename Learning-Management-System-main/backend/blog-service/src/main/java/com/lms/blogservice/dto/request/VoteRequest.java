package com.lms.blogservice.dto.request;

import com.lms.blogservice.enums.VoteType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteRequest {

    @NotNull(message = "VOTE_TYPE_NULL")
    VoteType voteType;
}
