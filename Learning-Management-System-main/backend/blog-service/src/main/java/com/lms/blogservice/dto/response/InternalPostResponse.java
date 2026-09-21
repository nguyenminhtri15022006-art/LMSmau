package com.lms.blogservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.blogservice.enums.PostStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalPostResponse {
    Long id;
    String title;
    String authorId;
    PostStatus status;
}
