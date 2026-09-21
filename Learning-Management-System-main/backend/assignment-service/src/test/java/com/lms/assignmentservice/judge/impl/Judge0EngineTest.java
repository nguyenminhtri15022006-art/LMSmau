package com.lms.assignmentservice.judge.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.assignmentservice.enums.SubmissionStatus;
import com.lms.assignmentservice.judge.JudgeEngineException;
import com.lms.assignmentservice.judge.JudgeRequest;
import com.lms.assignmentservice.judge.JudgeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Judge0EngineTest {

    private static final String BASE_URL = "http://judge0.test";

    Judge0Engine engine;
    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        engine = new Judge0Engine(new ObjectMapper());
        ReflectionTestUtils.setField(engine, "engine", "judge0");
        ReflectionTestUtils.setField(engine, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(engine, "apiKey", "");
        ReflectionTestUtils.setField(engine, "maxPollAttempts", 30);
        ReflectionTestUtils.setField(engine, "pollIntervalMs", 0L);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(engine, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void judgeUsesBase64AndDecodesCompileError() {
        String sourceCode = "print('wrong language')";
        String compileOutput = """
                main.c:1:3: error: invalid preprocessing directive #Python
                    1 | # Python 3
                      |   ^~~~~~
                main.c:2:1: error: unknown type name 'def'
                """;

        expectSubmit(sourceCode);
        expectPoll("""
                {
                  "submissions": [
                    {
                      "token": "tok1",
                      "status": { "id": 6, "description": "Compilation Error" },
                      "stdout": null,
                      "stderr": null,
                      "compile_output": "%s",
                      "time": "0.000",
                      "memory": 0,
                      "message": null
                    }
                  ]
                }
                """.formatted(jsonString(chunkedBase64(compileOutput))));

        JudgeResult result = engine.judge(buildRequest(sourceCode));

        assertThat(result.getOverallStatus()).isEqualTo(SubmissionStatus.CE);
        assertThat(result.getCompileError()).isEqualTo(compileOutput);
        assertThat(result.getCompileError()).doesNotContain("bWFpbi5j");
        assertThat(result.getTestCaseResults()).isEmpty();
        server.verify();
    }

    @Test
    void judgeDecodesRuntimeErrorOutputSnippet() {
        String stderr = "Traceback: runtime failed";

        expectSubmit("raise error");
        expectPoll("""
                {
                  "submissions": [
                    {
                      "token": "tok1",
                      "status": { "id": 11, "description": "Runtime Error" },
                      "stdout": null,
                      "stderr": "%s",
                      "compile_output": null,
                      "time": "0.010",
                      "memory": 1024,
                      "message": null
                    }
                  ]
                }
                """.formatted(base64(stderr)));

        JudgeResult result = engine.judge(buildRequest("raise error"));

        assertThat(result.getOverallStatus()).isEqualTo(SubmissionStatus.RE);
        assertThat(result.getTestCaseResults()).hasSize(1);
        assertThat(result.getTestCaseResults().getFirst().getStatus()).isEqualTo(SubmissionStatus.RE);
        assertThat(result.getTestCaseResults().getFirst().getOutputSnippet()).isEqualTo(stderr);
        server.verify();
    }

    @Test
    void judgeStopsPollingImmediatelyOnClientError() {
        expectSubmit("int main(void) { return 0; }");
        server.expect(requestTo(BASE_URL + "/submissions/batch?tokens=tok1&base64_encoded=true&fields=token,status,stdout,stderr,compile_output,time,memory,message"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"some attributes cannot be converted to UTF-8\"}"));

        assertThatThrownBy(() -> engine.judge(buildRequest("int main(void) { return 0; }")))
                .isInstanceOfSatisfying(JudgeEngineException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(JudgeEngineException.ErrorType.INVALID_RESPONSE);
                    assertThat(ex.getMessage()).contains("Judge0 HTTP 400");
                });
        server.verify();
    }

    private void expectSubmit(String sourceCode) {
        server.expect(requestTo(BASE_URL + "/submissions/batch?base64_encoded=true&wait=false"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString(base64(sourceCode))))
                .andRespond(withSuccess("[{\"token\":\"tok1\"}]", MediaType.APPLICATION_JSON));
    }

    private void expectPoll(String responseBody) {
        server.expect(requestTo(BASE_URL + "/submissions/batch?tokens=tok1&base64_encoded=true&fields=token,status,stdout,stderr,compile_output,time,memory,message"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private JudgeRequest buildRequest(String sourceCode) {
        return JudgeRequest.builder()
                .language("C")
                .sourceCode(sourceCode)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .testCases(List.of(JudgeRequest.TestCaseInput.builder()
                        .testCaseId(1L)
                        .input("")
                        .expectedOutput("")
                        .scoreWeight(1.0f)
                        .hidden(false)
                        .build()))
                .build();
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String chunkedBase64(String value) {
        return Base64.getMimeEncoder(24, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String jsonString(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n");
    }
}
