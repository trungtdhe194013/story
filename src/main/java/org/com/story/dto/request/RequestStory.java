package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String summary;

    private String coverUrl;
}
