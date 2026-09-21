package com.lms.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponse {
    private String courseId;
    private int    totalLessons;
    private long   completedLessons;
    private double percentComplete;
    private List<String> completedLessonIds;
}