package com.lms.assignmentservice.document;

import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "submissions")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_user_problem_time",
                def = "{'user_id': 1, 'problem_id': 1, 'submitted_at': -1}"
        ),
        // Tính gradebook: best score của student trong course
        @CompoundIndex(
                name = "idx_user_course_score",
                def = "{'user_id': 1, 'course_id': 1, 'score': -1}"
        ),
        // Thống kê acceptance rate của problem
        @CompoundIndex(
                name = "idx_problem_status",
                def = "{'problem_id': 1, 'status': 1}"
        ),
        // Polling PENDING submissions (background job)
        @CompoundIndex(
                name = "idx_pending_poll",
                def = "{'status': 1, 'submitted_at': 1}"
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Submission {
    @Id
    String id; // ObjectId — MongoDB tự sinh, dùng làm submissionId trong API

    // ===== Thông tin định danh =====
    @Field("problem_id")
    Integer problemId; // FK logic → problems.problem_id (MySQL)

    @Field("user_id")
    @Indexed
    String userId; // FK logic → Identity Service

    @Field("course_id")
    String courseId;

    // ===== Thông tin nộp bài =====
    @Field("language") // "JAVA" | "PYTHON" | "CPP" | "JAVASCRIPT"
            String language;

    @Field("source_code")
    String sourceCode;

    @Field("status")
    String status;
    /*
     * Các trạng thái có thể:
     * PENDING         — vừa tạo, chưa gửi Judge0
     * JUDGING         — đang poll Judge0
     * AC   — accepted toàn bộ test case
     * WA   — sai output
     * TLE  — quá thời gian
     * MLE  — quá bộ nhớ
     * RE   — runtime error
     * CE   — compilation error
     * IE   — Judge0 timeout hoặc lỗi hệ thống
     */

    @Field("score")
    Integer score = 0;

    @Field("exec_time_ms")
    Integer execTimeMs;

    @Field("memory_used_kb")
    Integer memoryUsedKb;

    // ===== Chi tiết từng test case =====
    @Field("test_case_result")
    List<TestCaseResult> testCaseResults;

    // Raw response từ Judge0 — schema thay đổi theo ngôn ngữ và phiên bản API
    // Lưu dạng Object để linh hoạt, không ép schema cứng
    @Field("judge_raw_response")
    Object judgeRawResponse;

    // null nếu compile thành công
    @Field("compile_error")
    String compileError;

    // Token để poll kết quả từ Judge0 (batch submission)
    @Field("judge0_token")
    String judge0Token;

    // ===== Timestamps =====
    @Field("submitted_at")
    Instant submittedAt;

    @Field("judge_at")
    Instant judgeAt;

    @Field("engine_used")
    String engineUsed;

    // ==================== Nested Document ====================

    /**
     * Kết quả của một test case trong lần chấm.
     * isHidden: dùng để filter khi trả response cho STUDENT
     * (student chỉ thấy kết quả public test case)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseResult {

        @Field("test_id")
        Long testId;

        @Field("status")
        String status;        // "AC" | "WA" | "TLE" | "MLE" | "RE" | "CE" | "IE"

        @Field("time_ms")
        Integer timeMs;

        @Field("memory_kb")
        Integer memoryKb;

        @Field("is_hidden")
        Boolean hidden;       // true = không trả output cho STUDENT

        // Chỉ lưu 200 ký tự đầu — tránh tốn storage cho output dài
        @Field("output_snippet")
        String outputSnippet;
    }
}
