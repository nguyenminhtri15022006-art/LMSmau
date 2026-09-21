package com.lms.blogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTagRequest {

    @NotBlank(message = "TAG_NAME_BLANK")
    @Size(max = 100, message = "TAG_NAME_SIZE")
    String name;

    @NotBlank(message = "TAG_SLUG_BLANK")
    @Size(max = 100, message = "TAG_SLUG_SIZE")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "TAG_SLUG_INVALID")
    String slug;
}
