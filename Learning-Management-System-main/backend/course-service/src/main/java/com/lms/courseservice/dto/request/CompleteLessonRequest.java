package com.lms.courseservice.dto.request;

import com.lms.courseservice.enums.LessonType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteLessonRequest {
    private LessonType lessonType;
}