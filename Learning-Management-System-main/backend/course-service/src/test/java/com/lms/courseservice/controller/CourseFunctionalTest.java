package com.lms.courseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.courseservice.configuration.SecurityConfig;
import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.response.CourseProgressResponse;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.EnrollmentResponse;
import com.lms.courseservice.dto.response.StructureNodeResponse;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.EnrollmentStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.enums.NodeType;
import com.lms.courseservice.exception.GlobalExceptionHandler;
import com.lms.courseservice.service.CourseService;
import com.lms.courseservice.service.CourseStructureService;
import com.lms.courseservice.service.LessonProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CourseController.class,
        CourseStructureController.class,
        LessonProgressController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CourseFunctionalTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CourseService courseService;
    @MockBean
    CourseStructureService courseStructureService;
    @MockBean
    LessonProgressService lessonProgressService;

    @Test
    void instructorStudentAndAdminCourseLearningWorkflow() throws Exception {
        when(courseService.createCourse(any()))
                .thenReturn(CourseResponse.builder()
                        .id("course-1")
                        .title("Java Basics")
                        .status(CourseStatus.PUBLIC)
                        .level(CourseLevel.BEGINNER)
                        .build());
        when(courseStructureService.addNode(eq("course-1"), any()))
                .thenReturn(CourseStructureResponse.builder()
                        .id("structure-1")
                        .courseId("course-1")
                        .nodes(List.of(StructureNodeResponse.builder()
                                .id("node-1")
                                .type(NodeType.LESSON)
                                .title("Intro")
                                .lessonId("lesson-1")
                                .build()))
                        .build());
        when(courseService.enrollCourse("course-1"))
                .thenReturn(EnrollmentResponse.builder()
                        .id(1L)
                        .courseId("course-1")
                        .userId("student-1")
                        .status(EnrollmentStatus.ACTIVE)
                        .build());
        when(lessonProgressService.getCourseProgress("course-1"))
                .thenReturn(CourseProgressResponse.builder()
                        .courseId("course-1")
                        .totalLessons(1)
                        .completedLessons(1)
                        .percentComplete(100.0)
                        .completedLessonIds(List.of("lesson-1"))
                        .build());
        when(courseService.lockCourse(eq("course-1"), any()))
                .thenReturn(CourseResponse.builder()
                        .id("course-1")
                        .title("Java Basics")
                        .status(CourseStatus.LOCKED)
                        .build());

        mockMvc.perform(post("/courses")
                        .headers(instructorHeaders())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Java Basics")
                        .param("status", "PUBLIC")
                        .param("level", "BEGINNER")
                        .param("duration", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("course-1"))
                .andExpect(jsonPath("$.data.status").value("PUBLIC"));

        mockMvc.perform(post("/courses/course-1/structure/nodes")
                        .headers(instructorHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "lesson",
                                  "title": "Intro"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes[0].lessonId").value("lesson-1"));

        mockMvc.perform(post("/courses/course-1/enroll")
                        .headers(studentHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("student-1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        CompleteLessonRequest completeLessonRequest = new CompleteLessonRequest();
        completeLessonRequest.setLessonType(LessonType.VIDEO);
        mockMvc.perform(post("/courses/course-1/lessons/lesson-1/complete")
                        .headers(studentHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(completeLessonRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/courses/course-1/progress")
                        .headers(studentHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentComplete").value(100.0))
                .andExpect(jsonPath("$.data.completedLessonIds[0]").value("lesson-1"));

        mockMvc.perform(patch("/courses/course-1/lock")
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                LockCourseRequest.builder().reason("content review").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        verify(courseService).createCourse(any());
        verify(courseStructureService).addNode(eq("course-1"), any());
        verify(courseService).enrollCourse("course-1");
        verify(lessonProgressService).completeLesson(eq("course-1"), eq("lesson-1"), any());
        verify(lessonProgressService).getCourseProgress("course-1");
        verify(courseService).lockCourse(eq("course-1"), any());
    }

    private HttpHeaders instructorHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "instructor-1");
        headers.add("X-User-Role", "ROLE_INSTRUCTOR");
        headers.add("X-User-Email", "instructor@example.com");
        return headers;
    }

    private HttpHeaders studentHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "student-1");
        headers.add("X-User-Role", "ROLE_STUDENT");
        headers.add("X-User-Email", "student@example.com");
        return headers;
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "admin-1");
        headers.add("X-User-Role", "ROLE_ADMIN");
        headers.add("X-User-Email", "admin@example.com");
        return headers;
    }
}
