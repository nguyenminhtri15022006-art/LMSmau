CREATE INDEX idx_courses_instructor_id ON courses (instructor_id);
CREATE INDEX idx_courses_status ON courses (status);
CREATE INDEX idx_courses_instructor_status ON courses (instructor_id, status);
CREATE INDEX idx_courses_title ON courses (title);
CREATE INDEX idx_courses_created_at ON courses (created_at DESC);

CREATE INDEX idx_course_learning_points_course_id ON course_learning_points (course_id);
CREATE INDEX idx_course_requirements_course_id ON course_requirements (course_id);

CREATE INDEX idx_enrollments_user_id ON enrollments (user_id);
CREATE INDEX idx_enrollments_course_id ON enrollments (course_id);
CREATE INDEX idx_enrollments_enrolled_at ON enrollments (enrolled_at DESC);
CREATE INDEX idx_enrollments_status ON enrollments (status);
CREATE INDEX idx_enrollments_user_status ON enrollments (user_id, status);

CREATE INDEX idx_schedules_course_id ON schedules (course_id);
CREATE INDEX idx_schedules_course_time ON schedules (course_id, start_time);

CREATE INDEX idx_announcements_course_id ON announcements (course_id);
CREATE INDEX idx_announcements_created_at ON announcements (created_at DESC);
CREATE INDEX idx_announcements_course_created ON announcements (course_id, created_at DESC);

CREATE INDEX idx_categories_slug ON categories (slug);

CREATE INDEX idx_cc_course_id ON course_categories (course_id);
CREATE INDEX idx_cc_category_id ON course_categories (category_id);
