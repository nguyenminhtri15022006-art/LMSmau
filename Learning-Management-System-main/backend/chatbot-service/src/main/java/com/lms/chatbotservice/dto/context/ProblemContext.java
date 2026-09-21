package com.lms.chatbotservice.dto.context;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemContext {
    Integer problemId;
    String title;
    String description;
    String difficulty;
    Integer timeLimitMs;
    Integer memoryLimitMb;
    List<String> allowedLangs;

    // Thông tin lần nộp gần nhất — có thể null nếu chưa nộp
    String latestSourceCode;
    String latestStatus;
    String latestLanguage;
    String compileError;
}