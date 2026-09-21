package com.lms.blogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCommentRequest {

    @NotBlank(message = "COMMENT_CONTENT_BLANK")
    String content;

    Long parentId;  // null nếu là comment gốc, có giá trị nếu là reply
}