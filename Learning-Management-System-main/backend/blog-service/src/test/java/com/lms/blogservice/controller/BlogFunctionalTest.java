package com.lms.blogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.blogservice.configuration.SecurityConfig;
import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.dto.response.VoteResponse;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
class BlogFunctionalTest {

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
    void authorPublishesPostStudentCommentsAndVotesWorkflow() throws Exception {
        when(tagService.createTag(any(CreateTagRequest.class)))
                .thenReturn(TagResponse.builder().id(1L).name("Java").slug("java").build());
        when(postService.createPost(any()))
                .thenReturn(PostResponse.builder()
                        .id(1L)
                        .title("Java Basics")
                        .slug("java-basics")
                        .authorId("author-1")
                        .status(PostStatus.PUBLISHED)
                        .build());
        when(postService.getPostBySlug("java-basics"))
                .thenReturn(PostResponse.builder()
                        .id(1L)
                        .title("Java Basics")
                        .slug("java-basics")
                        .status(PostStatus.PUBLISHED)
                        .viewCount(1L)
                        .build());
        when(commentService.createComment(eq(1L), any(CreateCommentRequest.class)))
                .thenReturn(CommentResponse.builder()
                        .id(10L)
                        .postId(1L)
                        .userId("student-1")
                        .content("Helpful")
                        .build());
        when(voteService.votePost(eq(1L), any(VoteRequest.class)))
                .thenReturn(VoteResponse.builder()
                        .targetId(1L)
                        .targetType(TargetType.POST)
                        .voteType(VoteType.UPVOTE)
                        .upvoteCount(1L)
                        .downvoteCount(0L)
                        .build());
        when(internalBlogService.existsPost(1L)).thenReturn(true);

        mockMvc.perform(post("/tags")
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                CreateTagRequest.builder().name("Java").slug("java").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("java"));

        mockMvc.perform(multipart("/posts")
                        .param("title", "Java Basics")
                        .param("content", "Long content")
                        .param("summary", "Intro")
                        .param("status", "PUBLISHED")
                        .param("tagIds", "1")
                        .headers(authorHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("java-basics"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/posts/public/java-basics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(post("/posts/1/comments")
                        .headers(studentHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                CreateCommentRequest.builder().content("Helpful").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Helpful"));

        mockMvc.perform(post("/posts/1/vote")
                        .headers(studentHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                VoteRequest.builder().voteType(VoteType.UPVOTE).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voteType").value("UPVOTE"))
                .andExpect(jsonPath("$.data.upvoteCount").value(1));

        mockMvc.perform(get("/internal/posts/1/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(tagService).createTag(any(CreateTagRequest.class));
        verify(postService).createPost(any());
        verify(postService).getPostBySlug("java-basics");
        verify(commentService).createComment(eq(1L), any(CreateCommentRequest.class));
        verify(voteService).votePost(eq(1L), any(VoteRequest.class));
        verify(internalBlogService).existsPost(1L);
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "admin-1");
        headers.add("X-User-Role", "ROLE_ADMIN");
        headers.add("X-User-Email", "admin@example.com");
        return headers;
    }

    private HttpHeaders authorHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "author-1");
        headers.add("X-User-Role", "ROLE_STUDENT");
        headers.add("X-User-Email", "author@example.com");
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
