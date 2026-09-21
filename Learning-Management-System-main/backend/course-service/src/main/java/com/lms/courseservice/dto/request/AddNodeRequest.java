package com.lms.courseservice.dto.request;

import com.lms.courseservice.enums.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddNodeRequest {

    @NotNull(message = "NODE_TYPE_BLANK")
    NodeType type;        // "folder" | "lesson"

    @NotBlank(message = "NODE_TITLE_BLANK")
    @Size(max = 500, message = "NODE_TITLE_SIZE")
    String title;

    String parentId;    // null nếu là root node

    Integer order;
}
