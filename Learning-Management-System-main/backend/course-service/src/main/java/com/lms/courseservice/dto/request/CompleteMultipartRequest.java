package com.lms.courseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteMultipartRequest {

    @NotBlank
    String uploadId;

    @NotBlank
    String objectName;

    @Min(1)
    int chunkCount;
}
