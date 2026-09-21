# EduLearn LMS

EduLearn LMS là hệ thống quản lý học tập hỗ trợ ba nhóm người dùng chính:
`STUDENT`, `INSTRUCTOR` và `ADMIN`.

Hệ thống cho phép học viên đăng ký khóa học, học bài, làm quiz, giải bài lập
trình, theo dõi tiến độ và tương tác qua blog. Giảng viên có thể tạo khóa học,
quản lý nội dung học tập, tạo bài kiểm tra, bài lập trình và theo dõi kết quả
học viên. Quản trị viên có thể quản lý người dùng, khóa học, blog, nội dung hệ
thống và trạng thái các service.

Truy cập: https://lmsystem.cloud để trải nghiệm ngay!

## Kiến Trúc Hệ Thống

Project được tổ chức theo mô hình monorepo:

```text
Learning-Management-System/
├── backend/                  # Spring Boot microservices
│   ├── service-registry/      # Eureka Server
│   ├── api-gateway/           # Spring Cloud Gateway
│   ├── identity-service/
│   ├── course-service/
│   ├── assignment-service/
│   ├── blog-service/
│   └── chatbot-service/
├── frontend/                 # Vite + React
├── docker/                   # Init/config files
├── docker-compose.yml         # Full local stack
├── docker-compose.db.yml      # Dependencies only
├── docker-compose.prod.yml    # Production-oriented stack
├── .env.example
└── API_REFERENCE.md
```

### Backend

Backend sử dụng kiến trúc microservices với Spring Boot:

| Service              |   Port | Vai trò                                                                       |
|----------------------|-------:|-------------------------------------------------------------------------------|
| `frontend`           | `5173` | Giao diện React, build production được serve bằng Nginx trong Docker          |
| `service-registry`   | `8761` | Eureka service discovery                                                      |
| `api-gateway`        | `8080` | Cổng vào API, route request, verify JWT, circuit breaker                      |
| `identity-service`   | `8081` | Auth, Google OAuth, email OTP, user/role, profile, admin user                 |
| `course-service`     | `8082` | Course, category, lesson, course structure, enrollment, schedule, Google Meet |
| `assignment-service` | `8083` | Quiz, question bank, coding problem, submission, judge, import template       |
| `blog-service`       | `8084` | Post, comment, tag, vote, thumbnail/file upload                               |
| `chatbot-service`    | `8085` | Chatbot học tập, lịch sử chat, sinh quiz/test case bằng LLM                   |

Tất cả request từ frontend đi qua `api-gateway` tại:

```text
http://localhost:8080
```

Các prefix chính qua gateway:

```text
/api/identity/**
/api/course/**
/api/assignment/**
/api/blog/**
/api/chatbot/**
```

## Hạ Tầng Local

Docker Compose cấu hình sẵn các dependency:

| Dependency    |    Port | Ghi chú                                                |
|---------------|--------:|--------------------------------------------------------|
| MySQL         |  `3306` | Dữ liệu quan hệ cho identity, course, assignment, blog |
| MongoDB       | `27017` | Dữ liệu document cho course, assignment, chatbot       |
| Redis         |  `6379` | Cache/rate limit cho chatbot và các luồng cần cache    |
| MinIO API     |  `9000` | Object storage cho avatar, ảnh, tài nguyên upload      |
| MinIO Console |  `9001` | Giao diện quản trị MinIO                               |

## Công Nghệ Chính

Backend:

- Java 21
- Spring Boot 3.3.5
- Spring Cloud 2023.0.1
- Spring Cloud Gateway, Eureka, OpenFeign
- Spring Security, JWT, Google OAuth
- Spring Data JPA, Spring Data MongoDB
- MySQL, MongoDB, Redis, MinIO
- Flyway, Maven, Resilience4j

Frontend:

- React 18
- Vite 5
- React Router 6
- Axios
- CodeMirror 6
- Framer Motion
- Vitest, Testing Library, ESLint

DevOps:

- Docker
- Docker Compose
- Nginx

## Cấu Hình Môi Trường

Tạo file `.env` từ mẫu:

```bash
cp .env.example .env
```

Trên Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Các giá trị cần kiểm tra hoặc thay trước khi chạy nghiêm túc:

- `JWT_SIGNER_KEY`: secret ký JWT.
- `MAIL_USERNAME`, `MAIL_PASSWORD`: Gmail/app password để gửi OTP.
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_ALLOWED_REDIRECT_URIS`: Google OAuth.
- `VITE_GOOGLE_CLIENT_ID`, `VITE_GOOGLE_REDIRECT_URI`: OAuth phía frontend.
- `LLM_PROVIDER`, `LLM_API_KEY`, `LLM_MODEL`, `LLM_BASE_URL`: chatbot và tính năng AI.
- `JUDGE_ENGINE`, `JUDGE0_URL`, `JUDGE0_API_KEY`: chấm bài lập trình.
- `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`: tài khoản MinIO local.

File `.env.example` đã có giá trị mặc định để chạy local, nhưng các khóa OAuth, mail, LLM và Judge0 là placeholder.

## Chạy Toàn Bộ Bằng Docker Compose

```bash
docker compose up --build
```

Sau khi các container healthy:

- Frontend: `http://localhost:5173`
- API Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- MinIO Console: `http://localhost:9001`

Dừng stack:

```bash
docker compose down
```

Dừng và xóa volume local:

```bash
docker compose down -v
```

## Chạy Dependency Bằng Docker, Backend/Frontend Bằng IDE

Nếu muốn chạy từng Spring Boot service bằng IntelliJ hoặc Maven, chỉ bật database/cache/storage:

```bash
docker compose -f docker-compose.db.yml up -d
```

Sau đó chạy backend theo thứ tự:

```bash
mvn spring-boot:run -f backend/pom.xml -pl service-registry
mvn spring-boot:run -f backend/pom.xml -pl api-gateway
mvn spring-boot:run -f backend/pom.xml -pl identity-service
mvn spring-boot:run -f backend/pom.xml -pl course-service
mvn spring-boot:run -f backend/pom.xml -pl assignment-service
mvn spring-boot:run -f backend/pom.xml -pl blog-service
mvn spring-boot:run -f backend/pom.xml -pl chatbot-service
```

Build toàn bộ backend:

```bash
mvn clean install -f backend/pom.xml
```

Build một module và các module phụ thuộc:

```bash
mvn clean install -f backend/pom.xml -pl assignment-service -am
```

## Chạy Frontend

```bash
cd frontend
npm install
npm run dev
```

Build production:

```bash
npm run build
```

Chạy test frontend:

```bash
npm run test
npm run test:coverage:core
```

Frontend dùng biến:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Nếu cần cấu hình riêng cho frontend:

```bash
cd frontend
cp .env.example .env
```

## Health Check

Một số URL hữu ích khi debug:

```text
http://localhost:8080/actuator/health/liveness
http://localhost:8081/identity/actuator/health
http://localhost:8082/course/actuator/health/liveness
http://localhost:8083/assignment/actuator/health/liveness
http://localhost:8084/blog/actuator/health/liveness
http://localhost:8085/chatbot/actuator/health/liveness
```

Admin dashboard cũng dùng các URL health này để hiển thị trạng thái service.

## API Reference

Tài liệu endpoint nằm ở:

```text
API_REFERENCE.md
```

Các endpoint thực tế nên được gọi qua gateway với prefix `/api/<service>`.

## Lệnh Kiểm Tra Và Tắt Port Trên Windows

Xem process đang listen trên các port chính của project:

```powershell
Get-NetTCPConnection -State Listen |
  Where-Object { $_.LocalPort -in 5173,8080,8081,8082,8083,8084,8085,8761 } |
  Select-Object LocalAddress,LocalPort,OwningProcess
```

Xem tên process theo PID:

```powershell
Get-Process -Id <PID> | Select-Object Id,ProcessName,Path
```

Tắt process:

```powershell
Stop-Process -Id <PID>
```

Nếu cần xem command line đầy đủ của process Java/Node:

```powershell
Get-CimInstance Win32_Process -Filter "ProcessId = <PID>" | Select-Object ProcessId,CommandLine
```

## Ghi Chú Vận Hành

- Chạy local đầy đủ cần bật Eureka trước các service còn lại.
- Gateway phụ thuộc Eureka để route `lb://...` đến các service.
- Các endpoint AI preview của assignment có timeout dài hơn qua gateway.
- `chatbot-service` cần MongoDB, Redis và cấu hình LLM hợp lệ để trả lời/sinh nội dung.
- `assignment-service` có thể dùng Judge0 hoặc custom judge tùy `JUDGE_ENGINE`.
- MinIO cần bucket tương ứng cho identity, course, assignment và blog; service sẽ dùng các biến bucket trong `.env`.
