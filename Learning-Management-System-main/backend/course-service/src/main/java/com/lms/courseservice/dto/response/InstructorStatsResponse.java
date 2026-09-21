package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorStatsResponse {
    long totalCourses;
    long totalStudents;
    long totalExercises;
}
