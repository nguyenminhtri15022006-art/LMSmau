package com.lms.courseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.courseservice.configuration.SecurityConfig;
import com.lms.courseservice.dto.request.CreateCategoryRequest;
import com.lms.courseservice.dto.response.CategoryResponse;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.InternalCourseResponse;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.GlobalExceptionHandler;
import com.lms.courseservice.service.CategoryService;
import com.lms.courseservice.service.CourseService;
import com.lms.courseservice.service.CourseStructureService;
import com.lms.courseservice.service.InternalCourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CategoryController.class,
        CourseController.class,
        CourseStructureController.class,
        InternalCourseController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CourseApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CategoryService categoryService;
    @MockBean
    CourseService courseService;
    @MockBean
    CourseStructureService courseStructureService;
    @MockBean
    InternalCourseService internalCourseService;

    @Test
    void publicGetEndpointsDoNotRequireGatewayHeaders() throws Exception {
        when(categoryService.getAllCategories())
                .thenReturn(List.of(CategoryResponse.builder().id(1L).name("Backend").build()));
        when(courseService.getAllCourses(any(Pageable.class), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(CourseResponse.builder()
                        .id("course-1")
                        .title("Java")
                        .status(CourseStatus.PUBLIC)
                        .build())));
        when(courseService.getCourseById("course-1"))
                .thenReturn(CourseResponse.builder().id("course-1").title("Java").build());
        when(courseStructureService.getStructure("course-1"))
                .thenReturn(CourseStructureResponse.builder().id("structure-1").courseId("course-1").build());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Backend"));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("course-1"));

        mockMvc.perform(get("/courses/course-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java"));

        mockMvc.perform(get("/courses/course-1/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("structure-1"));
    }

    @Test
    void privateEndpointRejectsRequestWithoutGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/courses/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2001));

        verify(courseService, never()).getMyCourses(any(Pageable.class));
    }

    @Test
    void studentRoleCannotAccessAdminCategoryCreation() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(studentHeaders())
                        .content(objectMapper.writeValueAsBytes(
                                CreateCategoryRequest.builder().name("Backend").slug("backend").build())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2002));

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    void adminEndpointDelegatesToServiceAndSerializesResponse() throws Exception {
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(CategoryResponse.builder().id(1L).name("Backend").slug("backend").build());

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(adminHeaders())
                        .content(objectMapper.writeValueAsBytes(
                                CreateCategoryRequest.builder().name("Backend").slug("backend").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Backend"));

        verify(categoryService).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void validationErrorReturnsCourseValidationCodeAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(adminHeaders())
                        .content(objectMapper.writeValueAsBytes(
                                CreateCategoryRequest.builder().name("").slug("backend").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2499))
                .andExpect(jsonPath("$.data.name").exists());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    void internalEndpointsDoNotRequireGatewayHeaders() throws Exception {
        when(internalCourseService.getCourse("course-1"))
                .thenReturn(InternalCourseResponse.builder()
                        .id("course-1")
                        .title("Java")
                        .instructorId("instructor-1")
                        .status(CourseStatus.PUBLIC)
                        .build());

        mockMvc.perform(get("/internal/courses/course-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("course-1"))
                .andExpect(jsonPath("$.status").value("PUBLIC"));

        verify(internalCourseService).getCourse(eq("course-1"));
    }

    private org.springframework.http.HttpHeaders adminHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-User-Id", "admin-1");
        headers.add("X-User-Role", "ROLE_ADMIN");
        headers.add("X-User-Email", "admin@example.com");
        return headers;
    }

    private org.springframework.http.HttpHeaders studentHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-User-Id", "student-1");
        headers.add("X-User-Role", "ROLE_STUDENT");
        headers.add("X-User-Email", "student@example.com");
        return headers;
    }
}
