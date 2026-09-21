package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitiateMultipartResponse {
    String uploadId;
    String objectName;
    List<String> presignedUrls;
}
