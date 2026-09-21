package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.AddNodeRequest;
import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.StructureNodeResponse;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mongo.StructureNode;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.NodeType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CourseStructureMapper;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseStructureServiceImplTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    CourseStructureRepository courseStructureRepository;
    @Mock
    LessonRepository lessonRepository;
    @Mock
    CourseStructureMapper courseStructureMapper;

    @InjectMocks
    CourseStructureServiceImpl courseStructureService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void addLessonNodeCreatesLessonDocumentAndAppendsNode() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .mongoStructureId("structure-1")
                .build();
        CourseStructure structure = CourseStructure.builder()
                .id("structure-1")
                .courseId("course-1")
                .nodes(new ArrayList<>())
                .build();
        Lesson savedLesson = Lesson.builder().id("lesson-1").courseId("course-1").build();

        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(courseStructureRepository.findById("structure-1")).thenReturn(Optional.of(structure));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);
        when(courseStructureRepository.save(structure)).thenReturn(structure);
        when(courseStructureMapper.toCourseStructureResponse(structure)).thenReturn(
                CourseStructureResponse.builder()
                        .id("structure-1")
                        .courseId("course-1")
                        .nodes(List.of(StructureNodeResponse.builder()
                                .type(NodeType.LESSON)
                                .title("Intro")
                                .lessonId("lesson-1")
                                .build()))
                        .build());

        CourseStructureResponse response = courseStructureService.addNode(
                "course-1",
                AddNodeRequest.builder().type(NodeType.LESSON).title("Intro").build());

        assertThat(response.getNodes()).singleElement()
                .extracting(StructureNodeResponse::getLessonId)
                .isEqualTo("lesson-1");
        assertThat(structure.getNodes()).singleElement().satisfies(node -> {
            assertThat(node.getType()).isEqualTo(NodeType.LESSON);
            assertThat(node.getLessonId()).isEqualTo("lesson-1");
            assertThat(node.getOrder()).isZero();
        });
    }

    @Test
    void addNodeRejectsMissingParent() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .mongoStructureId("structure-1")
                .build();
        CourseStructure structure = CourseStructure.builder()
                .id("structure-1")
                .courseId("course-1")
                .nodes(new ArrayList<>())
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(courseStructureRepository.findById("structure-1")).thenReturn(Optional.of(structure));

        assertThatThrownBy(() -> courseStructureService.addNode(
                "course-1",
                AddNodeRequest.builder()
                        .type(NodeType.FOLDER)
                        .title("Child")
                        .parentId("missing")
                        .build()))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARENT_NODE_NOT_FOUND);

        verify(courseStructureRepository, never()).save(any());
    }

    @Test
    void addNodeRejectsLockedCourse() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.LOCKED)
                .mongoStructureId("structure-1")
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseStructureService.addNode(
                "course-1",
                AddNodeRequest.builder().type(NodeType.FOLDER).title("Locked").build()))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_LOCKED);

        verify(courseStructureRepository, never()).findById("structure-1");
    }

    @Test
    void deleteNodeDeletesDescendantLessonsAndKeepsUnrelatedNodes() {
        TestSecurity.authenticate("instructor-1", "ROLE_INSTRUCTOR");
        Course course = Course.builder()
                .id("course-1")
                .instructorId("instructor-1")
                .status(CourseStatus.PUBLIC)
                .mongoStructureId("structure-1")
                .build();
        StructureNode folder = StructureNode.builder().id("folder-1").type(NodeType.FOLDER).title("Week 1").build();
        StructureNode lesson = StructureNode.builder()
                .id("lesson-node-1")
                .type(NodeType.LESSON)
                .parentId("folder-1")
                .lessonId("lesson-1")
                .build();
        StructureNode nestedLesson = StructureNode.builder()
                .id("lesson-node-2")
                .type(NodeType.LESSON)
                .parentId("lesson-node-1")
                .lessonId("lesson-2")
                .build();
        StructureNode unrelated = StructureNode.builder().id("folder-2").type(NodeType.FOLDER).title("Week 2").build();
        CourseStructure structure = CourseStructure.builder()
                .id("structure-1")
                .courseId("course-1")
                .nodes(new ArrayList<>(List.of(folder, lesson, nestedLesson, unrelated)))
                .build();

        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(courseStructureRepository.findById("structure-1")).thenReturn(Optional.of(structure));

        courseStructureService.deleteNode("course-1", "folder-1");

        verify(lessonRepository).deleteById("lesson-1");
        verify(lessonRepository).deleteById("lesson-2");
        ArgumentCaptor<CourseStructure> captor = ArgumentCaptor.forClass(CourseStructure.class);
        verify(courseStructureRepository).save(captor.capture());
        assertThat(captor.getValue().getNodes())
                .extracting(StructureNode::getId)
                .containsExactly("folder-2");
    }

    @Test
    void getLessonNodesSortsLessonNodesByOrder() {
        Course course = Course.builder().id("course-1").build();
        StructureNode second = StructureNode.builder().id("node-2").type(NodeType.LESSON).order(2).build();
        StructureNode first = StructureNode.builder().id("node-1").type(NodeType.LESSON).order(1).build();
        CourseStructure structure = CourseStructure.builder()
                .courseId("course-1")
                .nodes(List.of(second, first))
                .build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(courseStructureRepository.findByCourseId("course-1")).thenReturn(Optional.of(structure));
        when(courseStructureMapper.toNodeResponse(first)).thenReturn(StructureNodeResponse.builder().id("node-1").build());
        when(courseStructureMapper.toNodeResponse(second)).thenReturn(StructureNodeResponse.builder().id("node-2").build());

        var nodes = courseStructureService.getLessonNodes("course-1");

        assertThat(nodes).extracting(StructureNodeResponse::getId).containsExactly("node-1", "node-2");
    }
}
