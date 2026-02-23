package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Min(value = 0, message = "Chapter order must be >= 0")
    private Integer chapterOrder;

    @Min(value = 0, message = "Coin price must be >= 0")
    private Integer coinPrice = 0;

    private LocalDateTime publishAt;
}
