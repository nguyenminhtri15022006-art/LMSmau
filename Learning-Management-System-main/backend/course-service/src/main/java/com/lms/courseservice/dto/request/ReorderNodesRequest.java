package com.lms.courseservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReorderNodesRequest {

    @NotEmpty(message = "NODE_LIST_EMPTY")
    List<NodeOrderItem> nodes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeOrderItem {
        String nodeId;
        String parentId;
        Integer order;
    }
}

