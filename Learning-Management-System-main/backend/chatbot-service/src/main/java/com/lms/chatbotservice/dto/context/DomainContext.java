package com.lms.chatbotservice.dto.context;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DomainContext {
    String userId;
    ProblemContext problem;         // UC2 — null nếu không phải PROBLEM
    List<CourseContext> courses;    // UC3 — null nếu không phải COURSE
}