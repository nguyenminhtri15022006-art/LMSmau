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
public class CreateCategoryRequest {

    @NotBlank(message = "CATEGORY_NAME_BLANK")
    @Size(max = 200, message = "CATEGORY_NAME_SIZE")
    String name;

    @Size(max = 200, message = "CATEGORY_SLUG_SIZE")
    String slug;

}
