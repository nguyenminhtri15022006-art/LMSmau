package com.lms.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateNodeTitleRequest {

    @NotBlank(message = "NODE_TITLE_BLANK")
    @Size(max = 500, message = "NODE_TITLE_SIZE")
    String title;
}