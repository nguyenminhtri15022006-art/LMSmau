package com.lms.assignmentservice.judge;

import java.util.List;

/**
 * Abstraction layer cho máy chấm code.
 * <p>
 * Mọi máy chấm (Judge0, Sphere Engine, tự build...) đều implement interface này.
 * SubmissionService chỉ biết đến JudgeEngine, không biết implementation cụ thể.
 * <p>
 * Open/Closed Principle: thêm engine mới → tạo class mới implement interface,
 * không sửa bất kỳ code nào đang chạy.
 */
public interface JudgeEngine {

    /**
     * Tên định danh của engine — dùng để config và logging.
     * Ví dụ: "judge0", "sphere", "custom"
     */
    String engineName();

    /**
     * Danh sách ngôn ngữ engine này hỗ trợ.
     * Dùng để validate trước khi submit.
     */
    List<String> supportedLanguages();

    /**
     * Chấm một batch test case cho một submission.
     *
     * @param request Toàn bộ thông tin cần để chấm
     * @return Kết quả từng test case
     * @throws JudgeEngineException nếu lỗi kết nối hoặc timeout
     */
    JudgeResult judge(JudgeRequest request) throws JudgeEngineException;

    /**
     * Kiểm tra engine có đang hoạt động không.
     * Dùng cho health check và circuit breaker.
     */
    boolean isHealthy();
}