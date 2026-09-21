package com.lms.courseservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteResultResponse {
    List<String> successIds;    // Mời thành công
    List<String> alreadyIds;    // Đã enroll rồi
    List<String> notFoundIds;   // userId không tồn tại
}
