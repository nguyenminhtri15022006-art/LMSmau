package com.lms.courseservice.dto.request;

import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCourseRequest {

    @NotBlank(message = "COURSE_TITLE_BLANK")
    @Size(max = 500, message = "COURSE_TITLE_SIZE")
    String title;

    String description;

    MultipartFile thumbnail;

    @NotNull(message = "COURSE_STATUS_NULL")
    CourseStatus status;

    @Min(value = 1, message = "COURSE_DURATION_MIN")
    Integer duration;

    CourseLevel level;

    List<Long> categoryIds;

    List<String> learningPoints;

    List<String> requirements;

}
