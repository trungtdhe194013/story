package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditorChapterEditRequest {

    @NotBlank(message = "Content is required")
    private String content;

    private String note; // ghi chú của editor
}

