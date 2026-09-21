-- ============================================================
-- ASSIGNMENT SERVICE — V2: Performance indexes
-- ============================================================

-- problems: lọc bài tập theo course
CREATE INDEX idx_problems_course ON problems (course_id);

-- problems: lọc bài tập theo người tạo
CREATE INDEX idx_problems_creator ON problems (created_by);

-- problems: lọc bài tập public theo độ khó
CREATE INDEX idx_problems_public ON problems (is_public, difficulty);

CREATE INDEX idx_problems_course_deleted ON problems (course_id, is_deleted);

CREATE INDEX idx_problems_public_deleted ON problems (is_public, is_deleted);

-- test_cases: lấy test case theo thứ tự khi chấm
CREATE INDEX idx_tc_problem ON test_cases (problem_id, is_hidden, order_index);

-- quizzes: lọc quiz theo course
CREATE INDEX idx_quiz_course ON quizzes (course_id);

-- quizzes: lọc quiz đã publish trong 1 course
CREATE INDEX idx_quiz_published ON quizzes (is_published, course_id);

CREATE INDEX idx_quizzes_course_deleted ON quizzes (course_id, is_deleted);

CREATE INDEX idx_quizzes_published_deleted ON quizzes (is_published, is_deleted);

-- questions: lọc câu hỏi theo người tạo
CREATE INDEX idx_q_creator ON questions (created_by);

-- questions: lọc câu hỏi theo chủ đề
CREATE INDEX idx_q_topic ON questions (topic);

-- quiz_attempts: lấy lịch sử làm bài của user trong 1 quiz
CREATE INDEX idx_attempt_user_quiz ON quiz_attempts (user_id, quiz_id, submitted_at);

-- quiz_attempts: lọc attempt theo trạng thái (cleanup IN_PROGRESS hết hạn)
CREATE INDEX idx_attempt_status ON quiz_attempts (status);

CREATE UNIQUE INDEX uq_one_in_progress ON quiz_attempts (in_progress_lock);

-- quiz_answer_records: load tất cả đáp án của 1 attempt
CREATE INDEX idx_record_attempt ON quiz_answer_records (attempt_id);
