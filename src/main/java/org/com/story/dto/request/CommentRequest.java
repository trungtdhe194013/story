package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull(message = "Chapter ID is required")
    private Long chapterId;

    @NotBlank(message = "Content is required")
    private String content;

    private Long parentId; // null nếu là comment gốc, có giá trị nếu là reply
}

