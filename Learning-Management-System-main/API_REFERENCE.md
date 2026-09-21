# LMS - API Reference

> Tài liệu này liệt kê toàn bộ endpoint của hệ thống, phân theo từng service.  
> Cập nhật: 2026-05-18

---

## Mục lục

1. [Identity Service](#1-identity-service)
2. [Course Service](#2-course-service)
3. [Assignment Service](#3-assignment-service)
4. [Blog Service](#4-blog-service)
5. [Chatbot Service](#5-chatbot-service)
6. [Cấu trúc Response chung](#6-cấu-trúc-response-chung)
7. [Enum chung](#7-enum-chung)

---

## 1. Identity Service

### 1.1 AuthenticationController — `/auth`

#### `POST /auth/register` — Đăng ký tài khoản mới

**Request Body** (`UserCreationRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `username` | String | 5+ ký tự, chỉ chữ/số/gạch dưới |
| `password` | String | 8+ ký tự, không có khoảng trắng |
| `fullname` | String | 3–50 ký tự |
| `email` | String | max 100, định dạng email hợp lệ |
| `phone` | String | 10 chữ số bắt đầu bằng 0, tùy chọn |
| `dob` | LocalDate | tuổi >= 6 |
| `role` | RegisterableRole | `STUDENT`, `INSTRUCTOR` |

**Response** `ApiResponse<UserCreationResponse>`:

| Field | Kiểu |
|-------|------|
| `userId` | String |
| `username` | String |
| `email` | String |
| `role` | String |

---

#### `POST /auth/login` — Đăng nhập

**Request Body** (`AuthenticationRequest`):

| Field | Kiểu | Ghi chú |
|-------|------|---------|
| `username` | String | |
| `password` | String | |
| `ipAddress` | String | Tùy chọn |
| `deviceInfo` | String | Tùy chọn |

**Response** `ApiResponse<AuthenticationResponse>`:

| Field | Kiểu |
|-------|------|
| `accessToken` | String |
| `refreshToken` | String |
| `accessTokenExpiry` | Date |
| `isNewUser` | Boolean |
| `requiresRoleSelection` | Boolean |
| `user` | UserFullResponse |

---

#### `POST /auth/social-login/{provider}` — Đăng nhập mạng xã hội

**Path Variable**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `provider` | String | Ví dụ: `google`, `facebook` |

**Request Body** (`SocialLoginRequest`):

| Field | Kiểu |
|-------|------|
| `idToken` | String |

**Response** `ApiResponse<AuthenticationResponse>` — xem cấu trúc ở trên.

---

#### `POST /auth/introspect` — Kiểm tra tính hợp lệ của token

**Request Body** (`IntrospectRequest`):

| Field | Kiểu |
|-------|------|
| `token` | String |

**Response** `ApiResponse<IntrospectResponse>`:

| Field | Kiểu |
|-------|------|
| `valid` | Boolean |

---

#### `POST /auth/refresh` — Làm mới access token

**Request Body** (`RefreshTokenRequest`):

| Field | Kiểu |
|-------|------|
| `refreshToken` | String |

**Response** `ApiResponse<AuthenticationResponse>` — xem cấu trúc ở trên.

---

#### `POST /auth/logout` — Đăng xuất

**Request Body** (`LogoutRequest`):

| Field | Kiểu |
|-------|------|
| `accessToken` | String |

**Response** `ApiResponse<Void>`

---

### 1.2 EmailVerificationController — `/auth`

#### `POST /auth/verify-email/send` — Gửi mã xác thực email

**Request Body** (`SendEmailVerificationRequest`):

| Field | Kiểu |
|-------|------|
| `email` | String |

**Response** `ApiResponse<Void>`

---

#### `POST /auth/verify-email` — Xác thực email bằng mã

**Request Body** (`VerifyEmailRequest`):

| Field | Kiểu |
|-------|------|
| `email` | String |
| `code` | String |

**Response** `ApiResponse<Void>`

---

#### `POST /auth/forgot-password` — Gửi mã đặt lại mật khẩu

**Request Body** (`ForgotPasswordRequest`):

| Field | Kiểu |
|-------|------|
| `email` | String |

**Response** `ApiResponse<Void>`

---

#### `POST /auth/reset-password` — Đặt lại mật khẩu

**Request Body** (`ResetPasswordRequest`):

| Field | Kiểu |
|-------|------|
| `email` | String |
| `code` | String |
| `newPassword` | String |

**Response** `ApiResponse<Void>`

---

### 1.3 UserController — `/users` _(Yêu cầu xác thực)_

#### `GET /users/myinfo` — Lấy thông tin người dùng hiện tại

**Response** `ApiResponse<UserFullResponse>`:

| Field | Kiểu |
|-------|------|
| `userId` | String |
| `providerId` | String |
| `username` | String |
| `fullname` | String |
| `email` | String |
| `phone` | String |
| `dob` | Date |
| `role` | String |
| `avatarUrl` | String |
| `bio` | String |
| `verified` | Boolean |
| `active` | Boolean |
| `roleSelected` | Boolean |
| `authProvider` | String |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |
| `lastLoginAt` | LocalDateTime |

---

#### `PUT /users/myinfo` — Cập nhật hồ sơ người dùng

**Request Body** (`UserUpdateRequest`):

| Field | Kiểu |
|-------|------|
| `fullname` | String |
| `phone` | String |
| `dob` | LocalDate |
| `bio` | String |
| `avatarUrl` | String |

**Response** `ApiResponse<UserFullResponse>`

---

#### `PATCH /users/myinfo/password` — Đổi mật khẩu

**Request Body** (`UserUpdatePasswordRequest`):

| Field | Kiểu |
|-------|------|
| `oldPassword` | String |
| `newPassword` | String |

**Response** `ApiResponse<UserFullResponse>`

---

#### `PATCH /users/myinfo/role` — Cập nhật vai trò người dùng

**Request Body** (`UserRoleUpdateRequest`):

| Field | Kiểu |
|-------|------|
| `role` | RegisterableRole |

**Response** `ApiResponse<UserFullResponse>`

---

### 1.4 AdminUserController — `/admin/users` _(Yêu cầu ADMIN)_

#### `GET /admin/users/` — Lấy danh sách tất cả người dùng (phân trang)

**Query Params**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `status` | SubmissionStatus | Optional, canonical only |
| `page` | int | |
| `size` | int | |
| `sort` | String | |
| `role` | String | Tùy chọn |
| `keyword` | String | Tùy chọn |

**Response** `ApiResponse<Page<UserSummaryResponse>>`:

`UserSummaryResponse`:

| Field | Kiểu |
|-------|------|
| `userId` | String |
| `username` | String |
| `fullname` | String |
| `email` | String |
| `avatarUrl` | String |
| `role` | String |
| `active` | Boolean |
| `createdAt` | LocalDateTime |
| `authProvider` | String |

---

#### `GET /admin/users/stats` — Thống kê người dùng

**Response** `ApiResponse<UserDashboardResponse>`:

| Field | Kiểu |
|-------|------|
| `totalUsers` | Long |
| `totalStudents` | Long |
| `totalInstructors` | Long |
| `totalAdmins` | Long |
| `activeUsers` | Long |
| `newUsersThisMonth` | Long |

---

#### `GET /admin/users/{id}` — Lấy thông tin người dùng theo ID

**Path Variable**: `id` (String)

**Response** `ApiResponse<UserFullResponse>`

---

#### `PATCH /admin/users/{id}/active` — Bật/tắt trạng thái active của người dùng

**Path Variable**: `id` (String)

**Response** `ApiResponse<UserFullResponse>`

---

### 1.5 InternalUserController — `/internal` _(Gọi nội bộ giữa các service)_

#### `GET /internal/user/{id}`

**Path Variable**: `id` (String)

**Response** `UserFullResponse`

---

#### `GET /internal/users/{userId}/exists`

**Path Variable**: `userId` (String)

**Response** `Boolean`

---

## 2. Course Service

### 2.1 CourseController — `/courses`

#### `GET /courses/` — Lấy danh sách khóa học (phân trang)

**Query Params**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `page` | int | |
| `size` | int | |
| `sort` | String | |
| `status` | CourseStatus | Tùy chọn |
| `categoryId` | Long | Tùy chọn |

**Response** `ApiResponse<Page<CourseResponse>>`:

`CourseResponse`:

| Field | Kiểu |
|-------|------|
| `id` | String |
| `title` | String |
| `description` | String |
| `instructorId` | String |
| `thumbnailUrl` | String |
| `status` | CourseStatus |
| `duration` | Integer |
| `level` | CourseLevel |
| `meetingUrl` | String |
| `categories` | List\<CategoryResponse\> |
| `exerciseCount` | Long | Có trong GET endpoints; `null` trên write operations |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |

---

#### `GET /courses/{id}` — Lấy khóa học theo ID

**Path Variable**: `id` (String)

**Response** `ApiResponse<CourseResponse>`

---

#### `POST /courses/` — Tạo khóa học _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateCourseRequest`, multipart/form-data):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `title` | String | max 500, bắt buộc |
| `description` | String | Tùy chọn |
| `thumbnail` | MultipartFile | Tùy chọn |
| `status` | CourseStatus | `PRIVATE`, `PUBLIC`, `LOCKED` |
| `duration` | Integer | >= 1, Tùy chọn |
| `level` | CourseLevel | `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, Tùy chọn |
| `categoryIds` | List\<Long\> | Tùy chọn |

**Response** `ApiResponse<CourseResponse>`

---

#### `PUT /courses/{id}` — Cập nhật khóa học _(ADMIN, INSTRUCTOR)_

**Path Variable**: `id` (String)

**Request Body** (`UpdateCourseRequest`, multipart/form-data) — tương tự CreateCourseRequest.

**Response** `ApiResponse<CourseResponse>`

---

#### `DELETE /courses/{id}` — Xóa khóa học _(ADMIN, INSTRUCTOR)_

**Path Variable**: `id` (String)

**Response** `ApiResponse<String>`

---

#### `GET /courses/my` — Lấy danh sách khóa học của giảng viên _(ADMIN, INSTRUCTOR)_

**Query Params**: `page`, `size`, `sort`

**Response** `ApiResponse<Page<CourseResponse>>`

---

#### `GET /courses/enrolled` — Lấy danh sách khóa học đã đăng ký

**Response** `ApiResponse<List<CourseResponse>>`

---

#### `POST /courses/{id}/enroll` — Đăng ký khóa học

**Path Variable**: `id` (String)

**Response** `ApiResponse<EnrollmentResponse>`:

| Field | Kiểu |
|-------|------|
| `enrollmentId` | String |
| `courseId` | String |
| `userId` | String |
| `enrolledAt` | LocalDateTime |

---

#### `POST /courses/{id}/invite` — Mời học sinh vào khóa học _(ADMIN, INSTRUCTOR)_

**Path Variable**: `id` (String)

**Request Body** (`InviteStudentRequest`):

| Field | Kiểu |
|-------|------|
| `emails` | List\<String\> |

**Response** `ApiResponse<InviteResultResponse>`:

| Field | Kiểu |
|-------|------|
| `successEmails` | List\<String\> |
| `failedEmails` | List\<String\> |

---

#### `PATCH /courses/{id}/lock` — Khóa khóa học _(ADMIN)_

**Path Variable**: `id` (String)

**Request Body** (`LockCourseRequest`):

| Field | Kiểu |
|-------|------|
| `reason` | String |

**Response** `ApiResponse<CourseResponse>`

---

#### `PATCH /courses/{id}/unlock` — Mở khóa khóa học _(ADMIN)_

**Path Variable**: `id` (String)

**Response** `ApiResponse<CourseResponse>`

---

#### `GET /courses/{id}/students/count` — Lấy tổng số sinh viên đã tham gia khóa học

**Path Variable**: `id` (String)

**Response** `ApiResponse<Long>`

---

#### `GET /courses/{id}/students` — Lấy danh sách sinh viên đã tham gia khóa học _(ADMIN, INSTRUCTOR)_

**Path Variable**: `id` (String)

**Query Params**: `page`, `size`, `sort`

**Response** `ApiResponse<Page<CourseStudentResponse>>`:

| Field | Kiểu |
|-------|------|
| `userId` | String |
| `username` | String |
| `fullname` | String |
| `avatarUrl` | String |
| `enrolledAt` | LocalDateTime |
| `enrollmentStatus` | String (`ACTIVE`, `CANCELLED`) |

---

#### `GET /courses/{id}/lessons/count` — Lấy tổng số lesson của khóa học

**Path Variable**: `id` (String)

**Response** `ApiResponse<Long>`

---

### 2.2 CategoryController — `/categories`

#### `GET /categories/` — Lấy tất cả danh mục

**Response** `ApiResponse<List<CategoryResponse>>`:

| Field | Kiểu |
|-------|------|
| `id` | Long |
| `name` | String |
| `slug` | String |
| `createdAt` | LocalDateTime |

---

#### `POST /categories/` — Tạo danh mục _(ADMIN)_

**Request Body** (`CreateCategoryRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `name` | String | bắt buộc |

**Response** `ApiResponse<CategoryResponse>`

---

#### `PUT /categories/{id}` — Cập nhật danh mục _(ADMIN)_

**Path Variable**: `id` (Long)

**Request Body** (`UpdateCategoryRequest`):

| Field | Kiểu |
|-------|------|
| `name` | String |

**Response** `ApiResponse<CategoryResponse>`

---

#### `DELETE /categories/{id}` — Xóa danh mục _(ADMIN)_

**Path Variable**: `id` (Long)

**Response** `ApiResponse<CategoryResponse>`

---

### 2.3 LessonController — `/courses/{courseId}/lessons`

#### `GET /courses/{courseId}/lessons/{lessonId}` — Lấy thông tin bài học

**Path Variables**: `courseId` (String), `lessonId` (String)

**Response** `ApiResponse<LessonResponse>`:

| Field | Kiểu |
|-------|------|
| `id` | String |
| `courseId` | String |
| `lessonType` | LessonType |
| `content` | Map\<String, Object\> |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |

---

#### `PUT /courses/{courseId}/lessons/{lessonId}` — Cập nhật bài học _(ADMIN, INSTRUCTOR)_

**Path Variables**: `courseId` (String), `lessonId` (String)

**Request Body** (`UpdateLessonRequest`):

| Field | Kiểu |
|-------|------|
| `lessonType` | LessonType |
| `content` | Map\<String, Object\> |

**Response** `ApiResponse<LessonResponse>`

---

### 2.4 AnnouncementController — `/courses/{courseId}/announcements`

#### `GET /courses/{courseId}/announcements/` — Lấy danh sách thông báo (phân trang)

**Path Variable**: `courseId` (String)

**Query Params**: `page`, `size`, `sort`

**Response** `ApiResponse<Page<AnnouncementResponse>>`:

| Field | Kiểu |
|-------|------|
| `id` | Long |
| `courseId` | String |
| `title` | String |
| `content` | String |
| `authorId` | String |
| `createdAt` | LocalDateTime |

---

#### `POST /courses/{courseId}/announcements/` — Tạo thông báo _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateAnnouncementRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `title` | String | bắt buộc |
| `content` | String | bắt buộc |

**Response** `ApiResponse<AnnouncementResponse>`

---

#### `DELETE /courses/{courseId}/announcements/{announcementId}` — Xóa thông báo _(ADMIN, INSTRUCTOR)_

**Path Variables**: `courseId` (String), `announcementId` (Long)

**Response** `ApiResponse<String>`

---

### 2.5 CourseStructureController — `/courses/{courseId}/structure`

#### `GET /courses/{courseId}/structure/` — Lấy cấu trúc khóa học

**Response** `ApiResponse<CourseStructureResponse>`:

| Field | Kiểu |
|-------|------|
| `courseId` | String |
| `nodes` | List\<StructureNodeResponse\> |

`StructureNodeResponse`:

| Field | Kiểu |
|-------|------|
| `nodeId` | String |
| `type` | String (`SECTION`, `LESSON`) |
| `title` | String |
| `orderIndex` | Integer |
| `lessonId` | String |
| `lessonType` | LessonType |
| `children` | List\<StructureNodeResponse\> |

---

#### `POST /courses/{courseId}/structure/nodes` — Thêm node cấu trúc _(ADMIN, INSTRUCTOR)_

**Request Body** (`AddNodeRequest`):

| Field | Kiểu |
|-------|------|
| `type` | String |
| `title` | String |
| `parentId` | String |
| `orderIndex` | Integer |
| `lessonType` | LessonType |

**Response** `ApiResponse<CourseStructureResponse>`

---

#### `POST /courses/{courseId}/structure/nodes/batch` — Thêm nhiều node _(ADMIN, INSTRUCTOR)_

**Request Body** (`AddMultipleNodesRequest`):

| Field | Kiểu |
|-------|------|
| `nodes` | List\<AddNodeRequest\> |

**Response** `ApiResponse<CourseStructureResponse>`

---

#### `PUT /courses/{courseId}/structure/reorder` — Sắp xếp lại các node _(ADMIN, INSTRUCTOR)_

**Request Body** (`ReorderNodesRequest`):

| Field | Kiểu |
|-------|------|
| `nodeOrders` | List\<NodeOrderItem\> |

`NodeOrderItem`:

| Field | Kiểu |
|-------|------|
| `nodeId` | String |
| `orderIndex` | Integer |
| `parentId` | String |

**Response** `ApiResponse<CourseStructureResponse>`

---

#### `GET /courses/{courseId}/structure/lessons` — Lấy danh sách node bài học

**Response** `ApiResponse<List<StructureNodeResponse>>`

---

#### `DELETE /courses/{courseId}/structure/nodes/{nodeId}` — Xóa node _(ADMIN, INSTRUCTOR)_

**Path Variables**: `courseId` (String), `nodeId` (String)

**Response** `ApiResponse<String>`

---

### 2.6 ScheduleController — `/courses/{courseId}/schedules`

#### `GET /courses/{courseId}/schedules/` — Lấy danh sách lịch học

**Response** `ApiResponse<List<ScheduleResponse>>`:

| Field | Kiểu |
|-------|------|
| `scheduleId` | Long |
| `courseId` | String |
| `title` | String |
| `description` | String |
| `startTime` | LocalDateTime |
| `endTime` | LocalDateTime |
| `meetingUrl` | String |
| `createdAt` | LocalDateTime |

---

#### `POST /courses/{courseId}/schedules/` — Tạo lịch học _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateScheduleRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `title` | String | bắt buộc |
| `description` | String | Tùy chọn |
| `startTime` | LocalDateTime | bắt buộc |
| `endTime` | LocalDateTime | bắt buộc |

**Response** `ApiResponse<ScheduleResponse>`

---

#### `PUT /courses/{courseId}/schedules/{scheduleId}` — Cập nhật lịch học _(ADMIN, INSTRUCTOR)_

**Path Variables**: `courseId` (String), `scheduleId` (Long)

**Request Body** (`UpdateScheduleRequest`) — tương tự CreateScheduleRequest.

**Response** `ApiResponse<ScheduleResponse>`

---

#### `DELETE /courses/{courseId}/schedules/{scheduleId}` — Xóa lịch học _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<String>`

---

### 2.7 LessonProgressController — `/courses/{courseId}`

#### `POST /courses/{courseId}/lessons/{lessonId}/complete` — Đánh dấu bài học hoàn thành

**Path Variables**: `courseId` (String), `lessonId` (String)

**Request Body** (`CompleteLessonRequest`):

| Field | Kiểu |
|-------|------|
| `timeSpentSeconds` | Integer |

**Response** `ApiResponse<Void>`

---

#### `GET /courses/{courseId}/progress` — Lấy tiến độ học của khóa học

**Path Variable**: `courseId` (String)

**Response** `ApiResponse<CourseProgressResponse>`:

| Field | Kiểu |
|-------|------|
| `courseId` | String |
| `totalLessons` | Integer |
| `completedLessons` | Integer |
| `percentComplete` | Double |
| `lessonProgresses` | List\<LessonProgressItem\> |

`LessonProgressItem`:

| Field | Kiểu |
|-------|------|
| `lessonId` | String |
| `completed` | Boolean |
| `completedAt` | LocalDateTime |

---

### 2.8 StorageController — `/storage`

#### `POST /storage/presigned-url` — Lấy URL upload file lên MinIO/S3

**Request Body** (`PresignedUrlRequest`):

| Field | Kiểu |
|-------|------|
| `fileName` | String |
| `fileType` | String |
| `folder` | String |

**Response** `ApiResponse<PresignedUrlResponse>`:

| Field | Kiểu |
|-------|------|
| `uploadUrl` | String |
| `fileUrl` | String |

---

### 2.9 InternalCourseController — `/internal` _(Gọi nội bộ)_

| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/internal/courses/{courseId}` | `courseId` | `InternalCourseResponse` |
| GET | `/internal/courses/{courseId}/lessons/{lessonId}` | `courseId`, `lessonId` | `InternalLessonResponse` |
| GET | `/internal/courses/{courseId}/enrollments/{userId}` | `courseId`, `userId` | `Boolean` |
| GET | `/internal/courses/{courseId}/exists` | `courseId` | `Boolean` |
| GET | `/internal/courses/{courseId}/lessons/{lessonId}/exists` | `courseId`, `lessonId` | `Boolean` |
| GET | `/internal/courses` | — | `ApiResponse<List<ChatbotCourseResponse>>` |
| GET | `/internal/courses/enrolled` | `userId` (query) | `ApiResponse<List<ChatbotCourseResponse>>` |

---

## 3. Assignment Service

### 3.1 QuizController — `/quizzes`

#### `POST /quizzes/courses/{courseId}` — Tạo quiz _(ADMIN, INSTRUCTOR)_

**Path Variable**: `courseId` (String)

**Request Body** (`CreateQuizRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `title` | String | max 300, bắt buộc |
| `lessonId` | String | Tùy chọn |
| `description` | String | Tùy chọn |
| `duration` | Integer | 1–300 phút, null = không giới hạn |
| `totalScore` | Short | 0–1000, mặc định 100 |
| `passScore` | Byte | 0–100, mặc định 50 |
| `maxAttempts` | Byte | mặc định 0 = không giới hạn |
| `shuffleQuestions` | Boolean | mặc định true |
| `shuffleAnswers` | Boolean | mặc định true |
| `showResult` | ShowResultType | `IMMEDIATELY`, `AFTER_SUBMIT`, `AFTER_DEADLINE` |
| `startTime` | LocalDateTime | Tùy chọn |
| `endTime` | LocalDateTime | Tùy chọn |

**Response** `ApiResponse<QuizDetailResponse>`:

| Field | Kiểu |
|-------|------|
| `quizId` | Integer |
| `courseId` | String |
| `lessonId` | String |
| `title` | String |
| `description` | String |
| `duration` | Integer |
| `totalScore` | Short |
| `passScore` | Byte |
| `maxAttempts` | Byte |
| `shuffleQuestions` | Boolean |
| `shuffleAnswers` | Boolean |
| `showResult` | String |
| `startTime` | LocalDateTime |
| `endTime` | LocalDateTime |
| `published` | Boolean |
| `questionCount` | Integer |
| `createdBy` | String |
| `createdAt` | LocalDateTime |

---

#### `GET /quizzes/instructor/courses/{courseId}` — Lấy danh sách quiz (góc nhìn giảng viên)

**Response** `ApiResponse<Page<QuizInstructorDetailResponse>>`

---

#### `GET /quizzes/student/courses/{courseId}` — Lấy danh sách quiz (góc nhìn sinh viên, chỉ PUBLISHED)

**Response** `ApiResponse<Page<QuizStudentDetailResponse>>`

---

#### `GET /quizzes/{quizId}` — Lấy quiz theo ID

**Response** `ApiResponse<QuizDetailResponse>`

---

#### `PUT /quizzes/{quizId}` — Cập nhật quiz _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateQuizRequest`) — tương tự tạo quiz.

**Response** `ApiResponse<QuizDetailResponse>`

---

#### `DELETE /quizzes/{quizId}` — Xóa quiz _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<Void>`

---

#### `PATCH /quizzes/{quizId}/publish` — Công bố quiz _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<Void>`

---

#### `PATCH /quizzes/{quizId}/unpublish` — Hủy công bố quiz _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<Void>`

---

#### `POST /quizzes/{quizId}/questions` — Thêm câu hỏi vào quiz _(ADMIN, INSTRUCTOR)_

**Request Body** (`AddBulkQuestionQuizRequest`):

| Field | Kiểu |
|-------|------|
| `questionIds` | List\<Integer\> |

**Response** `ApiResponse<Void>`

---

#### `POST /quizzes/{quizId}/clone` — Nhân bản quiz _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<CloneQuizResponse>`:

| Field | Kiểu |
|-------|------|
| `newQuizId` | Integer |
| `title` | String |

---

#### `DELETE /quizzes/{quizId}/questions/{questionId}` — Xóa câu hỏi khỏi quiz _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<Void>`

---

### 3.2 QuestionController — `/questions` _(Yêu cầu ADMIN hoặc INSTRUCTOR)_

#### `POST /questions/courses/{courseId}` — Tạo câu hỏi

**Request Body** (`CreateQuestionRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `content` | String | bắt buộc |
| `type` | QuestionType | `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE` |
| `topic` | String | Tùy chọn |
| `explanation` | String | Tùy chọn |
| `imageUrl` | String | Tùy chọn |
| `score` | Byte | mặc định 10 |
| `answers` | List\<AnswerRequest\> | |

`AnswerRequest`:

| Field | Kiểu |
|-------|------|
| `content` | String |
| `correct` | Boolean |
| `orderIndex` | Short |

**Response** `ApiResponse<QuestionDetailResponse>`:

| Field | Kiểu |
|-------|------|
| `questionId` | Integer |
| `courseId` | String |
| `topic` | String |
| `type` | QuestionType |
| `content` | String |
| `explanation` | String |
| `imageUrl` | String |
| `score` | Short |
| `usedInQuizCount` | Long |
| `createdAt` | LocalDateTime |
| `createdBy` | String |
| `answers` | List\<AnswerDetailResponse\> |

`AnswerDetailResponse`:

| Field | Kiểu |
|-------|------|
| `answerId` | Integer |
| `content` | String |
| `correct` | Boolean |
| `orderIndex` | Short |

---

#### `POST /questions/courses/{courseId}/batch` — Import câu hỏi hàng loạt

**Request Body** (`ImportBulkQuestionRequest`):

| Field | Kiểu |
|-------|------|
| `questions` | List\<CreateQuestionRequest\> |

**Response** `ApiResponse<Page<QuestionDetailResponse>>`

---

#### `GET /questions/courses/{courseId}` — Lấy danh sách câu hỏi của tôi

**Query Params**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `topic` | String | Tùy chọn |
| `type` | QuestionType | Tùy chọn |
| `page` | int | |
| `size` | int | |

**Response** `ApiResponse<Page<QuestionDetailResponse>>`

---

#### `GET /questions/courses/{courseId}/topics` — Lấy danh sách topic câu hỏi

**Response** `ApiResponse<List<String>>`

---

#### `GET /questions/{questionId}` — Lấy câu hỏi theo ID

**Response** `ApiResponse<QuestionDetailResponse>`

---

#### `PUT /questions/{questionId}` — Cập nhật câu hỏi

**Request Body** (`CreateQuestionRequest`)

**Response** `ApiResponse<QuestionDetailResponse>`

---

#### `DELETE /questions/{questionId}` — Xóa câu hỏi

**Response** `ApiResponse<Void>`

---

### 3.3 QuizAttemptController — `/quizzes/{quizId}/attempts`

#### `POST /quizzes/{quizId}/attempts` — Bắt đầu lần làm quiz

**Response** `ApiResponse<QuizAttemptResponse>`:

| Field | Kiểu |
|-------|------|
| `attemptId` | Long |
| `quizId` | Integer |
| `quizTitle` | String |
| `duration` | Integer |
| `totalScore` | Short |
| `startedAt` | LocalDateTime |
| `expiresAt` | LocalDateTime |
| `status` | String (`IN_PROGRESS`, `SUBMITTED`, `EXPIRED`) |
| `questions` | List\<AttemptQuestionItem\> |

`AttemptQuestionItem` _(không có trường `correct` để tránh gian lận)_:

| Field | Kiểu |
|-------|------|
| `questionId` | Integer |
| `content` | String |
| `explanation` | String |
| `imageUrl` | String |
| `type` | String |
| `score` | Short |
| `answers` | List\<AttemptAnswerItem\> |
| `selectedAnswerIds` | List\<Integer\> |

---

#### `GET /quizzes/{quizId}/attempts/{attemptId}` — Lấy lần làm đang dở (hỗ trợ F5)

**Response** `ApiResponse<QuizAttemptResponse>`

---

#### `PUT /quizzes/{quizId}/attempts/{attemptId}` — Tự động lưu đáp án

**Request Body** (`AutoSaveAttemptRequest`):

| Field | Kiểu |
|-------|------|
| `answers` | List\<AnswerItem\> |

`AnswerItem`:

| Field | Kiểu |
|-------|------|
| `questionId` | Integer |
| `selectedAnswerIds` | List\<Integer\> |

**Response** `ApiResponse<Void>`

---

#### `POST /quizzes/{quizId}/attempts/{attemptId}/submit` — Nộp bài quiz

**Request Body** (`SubmitAttemptRequest`) — tương tự AutoSaveAttemptRequest.

**Response** `ApiResponse<QuizResultResponse>`:

| Field | Kiểu |
|-------|------|
| `attemptId` | Long |
| `quizId` | Integer |
| `quizTitle` | String |
| `score` | BigDecimal |
| `totalScore` | Short |
| `passed` | Boolean |
| `startedAt` | LocalDateTime |
| `submittedAt` | LocalDateTime |
| `timeSpentSeconds` | Integer |
| `questions` | List\<ResultQuestionItem\> |

`ResultQuestionItem`:

| Field | Kiểu |
|-------|------|
| `questionId` | Integer |
| `content` | String |
| `explanation` | String |
| `type` | String |
| `score` | Short |
| `earnedScore` | BigDecimal |
| `answers` | List\<ResultAnswerItem\> |
| `selectedAnswerIds` | List\<Integer\> |

`ResultAnswerItem` _(có trường `correct` sau khi nộp bài)_:

| Field | Kiểu |
|-------|------|
| `answerId` | Integer |
| `content` | String |
| `correct` | Boolean |
| `orderIndex` | Short |

---

#### `GET /quizzes/{quizId}/attempts/{attemptId}/result` — Lấy kết quả quiz

**Response** `ApiResponse<QuizResultResponse>`

---

#### `GET /quizzes/{quizId}/attempts` — Lịch sử làm bài

**Query Params**: `userId` (tùy chọn, INSTRUCTOR/ADMIN), `page`, `size`

**Response** `ApiResponse<Page<AttemptHistoryResponse>>`:

| Field | Kiểu |
|-------|------|
| `attemptId` | Long |
| `attemptNumber` | Byte |
| `score` | BigDecimal |
| `totalScore` | Short |
| `passed` | Boolean |
| `status` | String |
| `startedAt` | LocalDateTime |
| `submittedAt` | LocalDateTime |
| `timeSpentSeconds` | Integer |

---

### 3.4 ProblemController — `/problems`

#### `POST /problems/` — Tạo bài tập lập trình _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateProblemRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `title` | String | max 300, bắt buộc |
| `description` | String | bắt buộc |
| `difficulty` | DifficultyType | `EASY`, `MEDIUM`, `HARD` |
| `courseId` | String | bắt buộc |
| `lessonId` | String | Tùy chọn |
| `timeLimitMs` | Integer | 50–10000, mặc định 2000 |
| `memoryLimitMb` | Integer | 16–512, mặc định 256 |
| `allowedLangs` | List\<String\> | bắt buộc |
| `score` | Short | mặc định 100 |
| `isPublic` | Boolean | mặc định false |

**Response** `ApiResponse<ProblemResponse>`:

| Field | Kiểu |
|-------|------|
| `problemId` | Integer |
| `courseId` | String |
| `lessonId` | String |
| `title` | String |
| `slug` | String |
| `description` | String |
| `difficulty` | DifficultyType |
| `timeLimitMs` | Integer |
| `memoryLimitMb` | Integer |
| `allowedLangs` | List\<String\> |
| `score` | Short |
| `isPublic` | Boolean |
| `totalSubmit` | Integer |
| `totalAccepted` | Integer |
| `createBy` | String |
| `createdAt` | LocalDateTime |

---

#### `GET /problems/` — Lấy danh sách bài tập

**Query Params**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `courseId` | String | bắt buộc |
| `difficulty` | DifficultyType | Tùy chọn |
| `page` | int | |
| `size` | int | |

**Response** `ApiResponse<Page<ProblemResponse>>`

---

#### `GET /problems/{problemId}` — Lấy bài tập theo ID

**Response** `ApiResponse<ProblemResponse>`

---

#### `PUT /problems/{problemId}` — Cập nhật bài tập _(ADMIN, INSTRUCTOR)_

**Request Body** (`UpdateProblemRequest`) — tương tự CreateProblemRequest.

**Response** `ApiResponse<ProblemResponse>`

---

#### `DELETE /problems/{problemId}` — Xóa bài tập _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<Void>`

---

### 3.5 TestCaseController — `/problems/{problemId}/testcases`

#### `POST /problems/{problemId}/testcases` — Tạo test case _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateTestCaseRequest`):

| Field | Kiểu |
|-------|------|
| `input` | String |
| `expectedOutput` | String |
| `isPublic` | Boolean |
| `explanation` | String |

**Response** `ApiResponse<TestCaseResponse>`:

| Field | Kiểu |
|-------|------|
| `testCaseId` | Long |
| `problemId` | Integer |
| `input` | String |
| `expectedOutput` | String |
| `isPublic` | Boolean |
| `explanation` | String |

---

#### `POST /problems/{problemId}/testcases/bulk` — Tạo nhiều test case _(ADMIN, INSTRUCTOR)_

**Request Body** `List<CreateTestCaseRequest>`

**Response** `ApiResponse<BulkTestCaseResponse>`:

| Field | Kiểu |
|-------|------|
| `created` | Integer |
| `testCases` | List\<TestCaseResponse\> |

---

#### `GET /problems/{problemId}/testcases` — Lấy danh sách test case _(ADMIN, INSTRUCTOR)_

**Response** `ApiResponse<List<TestCaseResponse>>`

---

#### `PUT /problems/{problemId}/testcases/{testId}` — Cập nhật test case _(ADMIN, INSTRUCTOR)_

**Request Body** (`CreateTestCaseRequest`)

**Response** `ApiResponse<TestCaseResponse>`

---

#### `DELETE /problems/{id}/testcases` — Xóa nhiều test case _(ADMIN, INSTRUCTOR)_

**Query Param**: `ids` (List\<Long\>)

**Response** `ApiResponse<Void>`

---

### 3.6 CodeJudgeController — `/submissions`

#### `POST /submissions/` — Nộp code

**Request Body** (`CreateSubmissionRequest`):

| Field | Kiểu |
|-------|------|
| `problemId` | Integer |
| `language` | String |
| `sourceCode` | String |

**Response** `ApiResponse<SubmissionResponse>`:

| Field | Kiểu | Ghi chú |
|-------|------|---------|
| `submissionId` | String | |
| `problemId` | Integer | |
| `userId` | String | |
| `language` | String | |
| `status` | String | `PENDING`, `JUDGING`, `AC`, `WA`, `TLE`, `MLE`, `RE`, `CE`, `IE` |
| `score` | Integer | |
| `execTimeMs` | Integer | |
| `memoryUsedKb` | Integer | |
| `submittedAt` | Instant | |
| `judgedAt` | Instant | |
| `publicTestResults` | List\<TestCaseResultResponse\> | Sinh viên chỉ thấy test công khai |
| `sourceCode` | String | Chỉ INSTRUCTOR/ADMIN |
| `allTestResults` | List\<TestCaseResultResponse\> | Chỉ INSTRUCTOR/ADMIN |
| `compileError` | String | |

---

#### `GET /submissions/{id}` — Lấy submission theo ID

**Response** `ApiResponse<SubmissionResponse>`

---

#### `GET /submissions/` — Lịch sử nộp bài

**Query Params**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `problemId` | Integer | bắt buộc |
| `userId` | String | Tùy chọn |
| `page` | int | |
| `size` | int | |

**Response** `ApiResponse<Page<SubmissionResponse>>`

---

### 3.7 InternalController — `/internal` _(Assignment Service)_

| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/internal/problems/{problemId}` | `problemId` | `ApiResponse<InternalProblemResponse>` |
| GET | `/internal/submissions/latest` | `problemId`, `userId` (query) | `ApiResponse<InternalSubmissionResponse>` |
| GET | `/internal/courses/{courseId}/exercise-count` | `courseId` | `ApiResponse<Long>` |
| POST | `/internal/courses/exercise-counts` | body: `List<String>` courseIds | `ApiResponse<Map<String, Long>>` |

> `exercise-count` / `exercise-counts` trả tổng = quiz published + coding problems. Được course-service gọi nội bộ để populate `exerciseCount` trong `CourseResponse`.

---

## 4. Blog Service

### 4.1 PostController — `/posts`

#### `GET /posts/public` — Lấy danh sách bài viết đã đăng

**Query Params**:

| Param | Kiểu | Ghi chú |
|-------|------|---------|
| `page` | int | |
| `size` | int | |
| `tagId` | Long | Tùy chọn |
| `keyword` | String | Tùy chọn |

**Response** `ApiResponse<Page<PostSummaryResponse>>`:

| Field | Kiểu |
|-------|------|
| `id` | Long |
| `title` | String |
| `slug` | String |
| `summary` | String |
| `thumbnail` | String |
| `authorId` | String |
| `status` | PostStatus |
| `viewCount` | Long |
| `commentCount` | Long |
| `upvoteCount` | Long |
| `downvoteCount` | Long |
| `tags` | List\<TagResponse\> |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |

---

#### `GET /posts/public/{slug}` — Lấy bài viết theo slug

**Response** `ApiResponse<PostResponse>` — tương tự PostSummaryResponse + `content` (String)

---

#### `GET /posts/admin` — Lấy tất cả bài viết _(ADMIN)_

**Query Params**: `page`, `size`, `status`, `tagId`, `keyword`

**Response** `ApiResponse<Page<PostSummaryResponse>>`

---

#### `GET /posts/{id}` — Lấy bài viết theo ID

**Response** `ApiResponse<PostResponse>`

---

#### `GET /posts/me` — Lấy bài viết của tôi

**Query Params**: `page`, `size`, `status`

**Response** `ApiResponse<Page<PostSummaryResponse>>`

---

#### `POST /posts/` — Tạo bài viết

**Request Body** (`CreatePostRequest`, multipart/form-data):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `title` | String | max 500, bắt buộc |
| `content` | String | bắt buộc |
| `summary` | String | max 1000 |
| `thumbnail` | MultipartFile | Tùy chọn, upload ảnh lên MinIO |
| `status` | PostStatus | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `tagIds` | List\<Long\> | Tùy chọn |

**Response** `ApiResponse<PostResponse>`

---

#### `PUT /posts/{id}` — Cập nhật bài viết

**Request Body** (`UpdatePostRequest`, multipart/form-data) — tương tự CreatePostRequest.

**Response** `ApiResponse<PostResponse>`

---

#### `DELETE /posts/{id}` — Xóa bài viết

**Response** `ApiResponse<String>`

---

### 4.2 CommentController — `/posts/{postId}/comments`

#### `GET /posts/{postId}/comments/` — Lấy danh sách bình luận (phân trang)

**Response** `ApiResponse<Page<CommentResponse>>`:

| Field | Kiểu |
|-------|------|
| `id` | Long |
| `postId` | Long |
| `content` | String |
| `authorId` | String |
| `upvoteCount` | Long |
| `downvoteCount` | Long |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |

---

#### `POST /posts/{postId}/comments/` — Tạo bình luận

**Request Body** (`CreateCommentRequest`):

| Field | Kiểu |
|-------|------|
| `content` | String |

**Response** `ApiResponse<CommentResponse>`

---

#### `PUT /posts/{postId}/comments/{commentId}` — Cập nhật bình luận

**Request Body** (`UpdateCommentRequest`):

| Field | Kiểu |
|-------|------|
| `content` | String |

**Response** `ApiResponse<CommentResponse>`

---

#### `DELETE /posts/{postId}/comments/{commentId}` — Xóa bình luận

**Response** `ApiResponse<String>`

---

### 4.3 TagController — `/tags`

#### `GET /tags/` — Lấy tất cả tag

**Response** `ApiResponse<List<TagResponse>>`:

| Field | Kiểu |
|-------|------|
| `id` | Long |
| `name` | String |
| `slug` | String |

---

#### `GET /tags/{id}` — Lấy tag theo ID

**Response** `ApiResponse<TagResponse>`

---

#### `GET /tags/search` — Tìm kiếm tag theo từ khóa

**Query Param**: `keyword` (String)

**Response** `ApiResponse<List<TagResponse>>`

---

#### `POST /tags/` — Tạo tag _(ADMIN)_

**Request Body** (`CreateTagRequest`):

| Field | Kiểu |
|-------|------|
| `name` | String |

**Response** `ApiResponse<TagResponse>`

---

#### `PUT /tags/{id}` — Cập nhật tag _(ADMIN)_

**Request Body** (`UpdateTagRequest`):

| Field | Kiểu |
|-------|------|
| `name` | String |

**Response** `ApiResponse<TagResponse>`

---

#### `DELETE /tags/{id}` — Xóa tag _(ADMIN)_

**Response** `ApiResponse<String>`

---

### 4.4 VoteController

#### `POST /posts/{postId}/vote` — Vote bài viết

**Path Variable**: `postId` (Long)

**Request Body** (`VoteRequest`):

| Field | Kiểu |
|-------|------|
| `voteType` | String (`UPVOTE`, `DOWNVOTE`) |

**Response** `ApiResponse<VoteResponse>`:

| Field | Kiểu |
|-------|------|
| `upvoteCount` | Long |
| `downvoteCount` | Long |
| `userVote` | String |

---

#### `POST /posts/{postId}/comments/{commentId}/vote` — Vote bình luận

**Path Variables**: `postId` (Long), `commentId` (Long)

**Request Body** (`VoteRequest`) — tương tự trên.

**Response** `ApiResponse<VoteResponse>`

---

### 4.5 InternalBlogController — `/internal` _(Blog Service)_

| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/internal/posts/{postId}` | `postId` | `ApiResponse<InternalPostResponse>` |
| GET | `/internal/posts/{postId}/exists` | `postId` | `ApiResponse<Boolean>` |

---

## 5. Chatbot Service

### 5.1 ChatController

#### `POST /chat` — Chat streaming (Server-Sent Events)

**Request Body** (`ChatRequest`):

| Field | Kiểu | Ràng buộc |
|-------|------|-----------|
| `contextType` | ContextType | `COURSE`, `PROBLEM`, `LESSON`, `GENERAL` |
| `contextRefId` | String | Tùy chọn |
| `sessionId` | String | Tùy chọn, nếu tiếp tục session cũ |
| `message` | String | max 2000, bắt buộc |

**Response** `Flux<String>` — Stream SSE

---

#### `POST /generate/quiz` — Tạo quiz từ AI _(INSTRUCTOR)_

**Request Body** (`GenerateQuizRequest`):

| Field | Kiểu |
|-------|------|
| `courseId` | String |
| `content` | String |
| `difficulty` | String |

**Response** `ApiResponse<GenerateQuizResponse>`:

| Field | Kiểu |
|-------|------|
| `quizzes` | List\<QuizQuestionResponse\> |

---

#### `POST /generate/testcase` — Tạo test case từ AI _(INSTRUCTOR)_

**Request Body** (`GenerateTestCaseRequest`):

| Field | Kiểu |
|-------|------|
| `problemId` | Integer |
| `description` | String |
| `examples` | List\<String\> |

**Response** `ApiResponse<GenerateTestCaseResponse>`:

| Field | Kiểu |
|-------|------|
| `testCases` | List\<TestCaseResponse\> |

`TestCaseResponse`:

| Field | Kiểu |
|-------|------|
| `input` | String |
| `output` | String |
| `explanation` | String |

---

#### `GET /sessions` — Lấy danh sách phiên chat (phân trang)

**Query Params**: `page`, `size`

**Response** `ApiResponse<Page<ChatSessionResponse>>`:

| Field | Kiểu |
|-------|------|
| `sessionId` | String |
| `title` | String |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |

---

#### `GET /sessions/{sessionId}/messages` — Lấy tin nhắn trong phiên

**Path Variable**: `sessionId` (String)

**Response** `ApiResponse<List<ChatMessageResponse>>`:

| Field | Kiểu |
|-------|------|
| `id` | String |
| `sessionId` | String |
| `role` | String (`USER`, `ASSISTANT`) |
| `content` | String |
| `createdAt` | LocalDateTime |

---

#### `POST /sessions/{sessionId}/archive` — Lưu trữ phiên chat

**Response** `ApiResponse<Void>`

---

#### `GET /internal/health` — Kiểm tra sức khỏe service

**Response** `ApiResponse<String>`

---

## 6. Cấu trúc Response chung

Tất cả response đều được bọc trong `ApiResponse<T>`:

```json
{
  "code": 1000,
  "message": "Success",
  "data": { ... }
}
```

| Field | Kiểu | Ghi chú |
|-------|------|---------|
| `code` | Integer | `1000` = thành công |
| `message` | String | Mô tả kết quả |
| `data` | T | Dữ liệu trả về, có thể null |

---

## 7. Enum chung

| Enum | Giá trị |
|------|---------|
| `CourseStatus` | `PRIVATE`, `PUBLIC`, `LOCKED` |
| `CourseLevel` | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `PostStatus` | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `LessonType` | `VIDEO`, `DOCUMENT`, `QUIZ`, `CODING`, `LIVE_CLASS` |
| `QuestionType` | `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE` |
| `DifficultyType` | `EASY`, `MEDIUM`, `HARD` |
| `ShowResultType` | `IMMEDIATELY`, `AFTER_SUBMIT`, `AFTER_DEADLINE` |
| `ContextType` | `COURSE`, `PROBLEM`, `LESSON`, `GENERAL` |
| `RegisterableRole` | `STUDENT`, `INSTRUCTOR` |
| `VoteType` | `UPVOTE`, `DOWNVOTE` |
| `SubmissionStatus` | `PENDING`, `JUDGING`, `AC`, `WA`, `TLE`, `MLE`, `RE`, `CE`, `IE` |
