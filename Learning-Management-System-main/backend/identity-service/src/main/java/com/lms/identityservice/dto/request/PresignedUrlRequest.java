package com.lms.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUrlRequest {
    @NotBlank(message = "INVALID_FILE_NAME")
    String fileName;

    @NotBlank(message = "INVALID_FILE_TYPE")
    String fileType;

    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\/]+$", message = "INVALID_FOLDER")
    String folder;
}
