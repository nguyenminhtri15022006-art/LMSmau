package com.lms.assignmentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeleteFileRequest {
    @NotBlank(message = "File URL khong duoc de trong")
    String fileUrl;
}
