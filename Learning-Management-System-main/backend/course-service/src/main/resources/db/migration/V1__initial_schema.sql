CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NULL,
    instructor_id VARCHAR(36) NOT NULL,
    duration INT NULL,
    level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NULL,
    thumbnail_url VARCHAR(1000) NULL,
    status ENUM('PRIVATE', 'PUBLIC', 'LOCKED') NOT NULL DEFAULT 'PRIVATE',
    meeting_url VARCHAR(500) NULL,
    google_event_id VARCHAR(200) NULL,
    mongo_structure_id VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_courses PRIMARY KEY (id),
    CONSTRAINT uq_courses_mongo_structure UNIQUE (mongo_structure_id)
);

CREATE TABLE IF NOT EXISTS course_categories (
    course_id VARCHAR(36) NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_course_categories PRIMARY KEY (course_id, category_id),
    CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_cc_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS course_learning_points (
    course_id VARCHAR(36) NOT NULL,
    point TEXT NULL,
    point_order INT NOT NULL,
    CONSTRAINT pk_course_learning_points PRIMARY KEY (course_id, point_order),
    CONSTRAINT fk_course_learning_points_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS course_requirements (
    course_id VARCHAR(36) NOT NULL,
    requirement TEXT NULL,
    requirement_order INT NOT NULL,
    CONSTRAINT pk_course_requirements PRIMARY KEY (course_id, requirement_order),
    CONSTRAINT fk_course_requirements_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    status ENUM('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    enrolled_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_enrollments PRIMARY KEY (id),
    CONSTRAINT uq_enrollments_user_course UNIQUE (user_id, course_id),
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(500) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    note TEXT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_schedules PRIMARY KEY (id),
    CONSTRAINT fk_sch_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_announcements PRIMARY KEY (id),
    CONSTRAINT fk_ann_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    lesson_id VARCHAR(36) NOT NULL,
    lesson_type ENUM('VIDEO', 'DOCUMENT') NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    last_accessed DATETIME NULL,
    CONSTRAINT pk_lesson_progress PRIMARY KEY (id),
    CONSTRAINT uq_student_lesson UNIQUE (student_id, lesson_id)
);
