package org.com.story.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingRequest {

    @NotNull(message = "Story ID is required")
    private Long storyId;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer score;

    private String review;
}

