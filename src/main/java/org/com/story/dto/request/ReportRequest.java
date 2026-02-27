package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {

    @NotBlank(message = "Target type is required (STORY, CHAPTER, COMMENT)")
    private String targetType; // STORY, CHAPTER, COMMENT

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotBlank(message = "Reason is required")
    private String reason;
}

