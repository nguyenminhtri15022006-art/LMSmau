package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.AddMultipleNodesRequest;
import com.lms.courseservice.dto.request.AddNodeRequest;
import com.lms.courseservice.dto.request.ReorderNodesRequest;
import com.lms.courseservice.dto.request.UpdateNodeTitleRequest;
import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.StructureNodeResponse;

import java.util.List;

public interface CourseStructureService {
    CourseStructureResponse getStructure(String courseId);
    CourseStructureResponse addNode(String courseId, AddNodeRequest request);
    CourseStructureResponse addNodes(String courseId, AddMultipleNodesRequest request);
    CourseStructureResponse reorderNodes(String courseId, ReorderNodesRequest request);
    void deleteNode(String courseId, String nodeId);
    CourseStructureResponse updateNodeTitle(String courseId, String nodeId, UpdateNodeTitleRequest request);
    int countLessons(String courseId);
    boolean isLessonInCourse(String courseId, String lessonId);
    List<StructureNodeResponse> getLessonNodes(String courseId);
}
