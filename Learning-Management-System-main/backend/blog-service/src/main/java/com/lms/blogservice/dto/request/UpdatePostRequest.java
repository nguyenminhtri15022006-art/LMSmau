package com.lms.blogservice.dto.request;

import com.lms.blogservice.enums.PostStatus;
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
public class UpdatePostRequest {

    @NotBlank(message = "POST_TITLE_BLANK")
    @Size(max = 500, message = "POST_TITLE_SIZE")
    String title;

    @NotBlank(message = "POST_CONTENT_BLANK")
    String content;

    @Size(max = 1000, message = "POST_SUMMARY_SIZE")
    String summary;

    MultipartFile thumbnail;

    @NotNull(message = "POST_STATUS_NULL")
    PostStatus status;

    List<Long> tagIds;

    Boolean clearTags;
}
