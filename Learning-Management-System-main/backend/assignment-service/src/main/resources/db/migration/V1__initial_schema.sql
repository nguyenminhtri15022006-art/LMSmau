-- ============================================================
-- ASSIGNMENT SERVICE — V1: Initial schema
-- ============================================================

CREATE TABLE problems
(
    problem_id      INT                           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id       CHAR(36)                      NOT NULL,
    lesson_id       CHAR(36)                      NOT NULL,
    title           VARCHAR(300)                  NOT NULL,
    slug            VARCHAR(350)                  NOT NULL UNIQUE,
    description     LONGTEXT                      NOT NULL,
    difficulty      ENUM ('EASY','MEDIUM','HARD') NOT NULL,
    time_limit_ms   INT                           NOT NULL DEFAULT 2000,
    memory_limit_mb INT                           NOT NULL DEFAULT 256,
    allowed_langs   JSON                          NOT NULL,
    score           SMALLINT                      NOT NULL DEFAULT 100,
    is_public       TINYINT(1)                    NOT NULL DEFAULT 0,
    is_deleted      TINYINT(1)                    NOT NULL DEFAULT 0,
    deleted_at      DATETIME                      NULL,
    deleted_by      CHAR(36)                      NULL,
    total_submit    INT                           NOT NULL DEFAULT 0,
    total_accepted  INT                           NOT NULL DEFAULT 0,
    created_by      CHAR(36)                      NOT NULL,
    created_at      DATETIME                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE test_cases
(
    test_id         BIGINT     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    problem_id      INT        NOT NULL,
    input           LONGTEXT   NOT NULL,
    expected_output LONGTEXT   NOT NULL,
    is_hidden       TINYINT(1) NOT NULL DEFAULT 1,
    order_index     SMALLINT   NOT NULL DEFAULT 0,
    score_weight    FLOAT      NOT NULL DEFAULT 1.0,
    created_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tc_problem FOREIGN KEY (problem_id) REFERENCES problems (problem_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quizzes
(
    quiz_id           INT                                                  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id         CHAR(36)                                             NOT NULL,
    lesson_id         CHAR(36)                                             NULL,
    title             VARCHAR(300)                                         NOT NULL,
    description       TEXT                                                 NULL,
    duration          INT                                                  NULL,
    total_score       SMALLINT                                             NOT NULL DEFAULT 100,
    pass_score        TINYINT                                              NOT NULL DEFAULT 50,
    max_attempts      TINYINT                                              NOT NULL DEFAULT 0,
    shuffle_questions TINYINT(1)                                           NOT NULL DEFAULT 1,
    shuffle_answers   TINYINT(1)                                           NOT NULL DEFAULT 1,
    show_result       ENUM ('IMMEDIATELY','AFTER_SUBMIT','AFTER_DEADLINE') NOT NULL DEFAULT 'AFTER_SUBMIT',
    start_time        DATETIME                                             NULL,
    end_time          DATETIME                                             NULL,
    created_by        CHAR(36)                                             NOT NULL,
    created_at        DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_published      TINYINT(1)                                           NOT NULL DEFAULT 0,
    is_deleted        TINYINT(1)                                           NOT NULL DEFAULT 0,
    deleted_at        DATETIME                                             NULL,
    deleted_by        CHAR(36)                                             NULL,
    updated_at        DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE questions
(
    question_id INT                                     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id   CHAR(36)                                NOT NULL COMMENT 'Ref → Course Service',
    content     TEXT                                    NOT NULL,
    type        ENUM ('SINGLE','MULTIPLE','TRUE_FALSE') NOT NULL,
    topic       VARCHAR(100)                            NULL,
    explanation TEXT                                    NULL,
    score       TINYINT                                 NOT NULL DEFAULT 10,
    image_url   VARCHAR(500)                            NULL,
    created_by  CHAR(36)                                NOT NULL,
    created_at  DATETIME                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE answers
(
    answer_id   INT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_id INT        NOT NULL,
    content     TEXT       NOT NULL,
    is_correct  TINYINT(1) NOT NULL DEFAULT 0,
    order_index SMALLINT   NOT NULL DEFAULT 0,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quiz_questions
(
    id             BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    quiz_id        INT      NOT NULL,
    question_id    INT      NOT NULL,
    override_score SMALLINT NULL COMMENT 'NULL = dùng score gốc của question',
    order_index    SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_quiz_question UNIQUE (quiz_id, question_id),
    CONSTRAINT fk_qq_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_qq_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quiz_attempts
(
    attempt_id     BIGINT                                     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    quiz_id        INT                                        NOT NULL,
    user_id        CHAR(36)                                   NOT NULL,
    score          FLOAT                                      NOT NULL DEFAULT 0,
    total_score    DECIMAL(10, 2)                             NOT NULL,
    is_passed      TINYINT(1)                                 NOT NULL DEFAULT 0,
    time_spent_s   INT                                        NULL,
    status         ENUM ('IN_PROGRESS','SUBMITTED','EXPIRED') NOT NULL DEFAULT 'IN_PROGRESS',
    started_at     DATETIME                                   NOT NULL,
    submitted_at   DATETIME                                   NULL,
    expires_at     DATETIME                                   NULL,
    attempt_number TINYINT                                    NOT NULL DEFAULT 1,
    in_progress_lock VARCHAR(100)
        GENERATED ALWAYS AS (IF(status = 'IN_PROGRESS', CONCAT(user_id, ':', quiz_id), NULL)) VIRTUAL,
    version        BIGINT                                     NOT NULL DEFAULT 0,
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quiz_answer_records
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    attempt_id          BIGINT         NOT NULL,
    question_id         INT            NOT NULL,
    selected_answer_ids JSON           NOT NULL,
    earned_score        DECIMAL(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_record UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_record_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_record_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE grades
(
    id              BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         CHAR(36) NOT NULL,
    course_id       CHAR(36) NOT NULL,
    quiz_id         INT      NOT NULL,
    best_score      FLOAT    NOT NULL DEFAULT 0,
    attempts_count  INT      NOT NULL DEFAULT 0,
    last_attempt_at DATETIME NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_quiz UNIQUE (user_id, quiz_id),
    CONSTRAINT fk_grade_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
