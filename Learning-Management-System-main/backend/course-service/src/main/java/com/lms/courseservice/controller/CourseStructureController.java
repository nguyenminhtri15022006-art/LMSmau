package com.lms.courseservice.controller;

import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.dto.request.AddMultipleNodesRequest;
import com.lms.courseservice.dto.request.AddNodeRequest;
import com.lms.courseservice.dto.request.ReorderNodesRequest;
import com.lms.courseservice.dto.request.UpdateNodeTitleRequest;
import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.StructureNodeResponse;
import com.lms.courseservice.service.CourseStructureService;

import java.util.List;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses/{courseId}/structure")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseStructureController {

    CourseStructureService courseStructureService;

    @GetMapping
    public ApiResponse<CourseStructureResponse> getStructure(
            @PathVariable String courseId) {
        return ApiResponse.<CourseStructureResponse>builder()
                .data(courseStructureService.getStructure(courseId))
                .build();
    }

    @PostMapping("/nodes")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<CourseStructureResponse> addNode(
            @PathVariable String courseId,
            @RequestBody @Valid AddNodeRequest request) {
        return ApiResponse.<CourseStructureResponse>builder()
                .data(courseStructureService.addNode(courseId, request))
                .build();
    }

    @PostMapping("/nodes/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<CourseStructureResponse> addNodes(
            @PathVariable String courseId,
            @RequestBody @Valid AddMultipleNodesRequest request) {
        return ApiResponse.<CourseStructureResponse>builder()
                .data(courseStructureService.addNodes(courseId, request))
                .build();
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<CourseStructureResponse> reorderNodes(
            @PathVariable String courseId,
            @RequestBody @Valid ReorderNodesRequest request) {
        return ApiResponse.<CourseStructureResponse>builder()
                .data(courseStructureService.reorderNodes(courseId, request))
                .build();
    }

    @GetMapping("/lessons")
    public ApiResponse<List<StructureNodeResponse>> getLessonNodes(
            @PathVariable String courseId) {
        return ApiResponse.<List<StructureNodeResponse>>builder()
                .data(courseStructureService.getLessonNodes(courseId))
                .build();
    }

    @PatchMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<CourseStructureResponse> updateNodeTitle(
            @PathVariable String courseId,
            @PathVariable String nodeId,
            @RequestBody @Valid UpdateNodeTitleRequest request) {
        return ApiResponse.<CourseStructureResponse>builder()
                .data(courseStructureService.updateNodeTitle(courseId, nodeId, request))
                .build();
    }

    @DeleteMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ApiResponse<String> deleteNode(
            @PathVariable String courseId,
            @PathVariable String nodeId) {
        courseStructureService.deleteNode(courseId, nodeId);
        return ApiResponse.<String>builder()
                .message("Xóa node thành công")
                .build();
    }
}
