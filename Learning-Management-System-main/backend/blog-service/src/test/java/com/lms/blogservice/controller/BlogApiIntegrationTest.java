package com.lms.blogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.blogservice.configuration.SecurityConfig;
import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.dto.response.InternalPostResponse;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.exception.GlobalExceptionHandler;
import com.lms.blogservice.service.CommentService;
import com.lms.blogservice.service.InternalBlogService;
import com.lms.blogservice.service.PostService;
import com.lms.blogservice.service.TagService;
import com.lms.blogservice.service.VoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
        PostController.class,
        CommentController.class,
        TagController.class,
        VoteController.class,
        InternalBlogController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class BlogApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostService postService;
    @MockBean
    CommentService commentService;
    @MockBean
    TagService tagService;
    @MockBean
    VoteService voteService;
    @MockBean
    InternalBlogService internalBlogService;

    @Test
    void publicGetEndpointsDoNotRequireGatewayHeaders() throws Exception {
        when(postService.getPosts(any(Pageable.class), eq(PostStatus.PUBLISHED), eq(null), eq(null)))
                .thenReturn(new PageImpl<>(List.of(PostSummaryResponse.builder()
                        .id(1L)
                        .title("Java")
                        .slug("java")
                        .status(PostStatus.PUBLISHED)
                        .build())));
        when(postService.getPostBySlug("java"))
                .thenReturn(PostResponse.builder().id(1L).title("Java").slug("java").build());
        when(tagService.getAllTags())
                .thenReturn(List.of(TagResponse.builder().id(1L).name("Java").slug("java").build()));
        when(commentService.getComments(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(CommentResponse.builder().id(10L).content("Nice").build())));

        mockMvc.perform(get("/posts/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].slug").value("java"));

        mockMvc.perform(get("/posts/public/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java"));

        mockMvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Java"));

        mockMvc.perform(get("/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Nice"));
    }

    @Test
    void privateEndpointRejectsRequestWithoutGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/posts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4001));

        verify(postService, never()).getMyPosts(any(Pageable.class), any());
    }

    @Test
    void studentRoleCannotCreateTag() throws Exception {
        mockMvc.perform(post("/tags")
                        .headers(studentHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                CreateTagRequest.builder().name("Java").slug("java").build())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4002));

        verify(tagService, never()).createTag(any());
    }

    @Test
    void adminCreateTagDelegatesToServiceAndSerializesCreatedResponse() throws Exception {
        when(tagService.createTag(any(CreateTagRequest.class)))
                .thenReturn(TagResponse.builder().id(1L).name("Java").slug("java").build());

        mockMvc.perform(post("/tags")
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                CreateTagRequest.builder().name("Java").slug("java").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.slug").value("java"));

        verify(tagService).createTag(any(CreateTagRequest.class));
    }

    @Test
    void validationErrorReturnsBlogValidationCodeAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/tags")
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                CreateTagRequest.builder().name("").slug("Invalid Slug").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4499))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.slug").exists());

        verify(tagService, never()).createTag(any());
    }

    @Test
    void internalEndpointsDoNotRequireGatewayHeaders() throws Exception {
        when(internalBlogService.getPost(1L))
                .thenReturn(InternalPostResponse.builder()
                        .id(1L)
                        .title("Java")
                        .authorId("author-1")
                        .status(PostStatus.PUBLISHED)
                        .build());

        mockMvc.perform(get("/internal/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(internalBlogService).getPost(1L);
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "admin-1");
        headers.add("X-User-Role", "ROLE_ADMIN");
        headers.add("X-User-Email", "admin@example.com");
        return headers;
    }

    private HttpHeaders studentHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "student-1");
        headers.add("X-User-Role", "ROLE_STUDENT");
        headers.add("X-User-Email", "student@example.com");
        return headers;
    }
}
