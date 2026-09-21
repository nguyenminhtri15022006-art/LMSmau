package com.lms.courseservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddMultipleNodesRequest {

    @NotEmpty(message = "NODE_LIST_EMPTY")
    List<@Valid AddNodeRequest> nodes;
}

