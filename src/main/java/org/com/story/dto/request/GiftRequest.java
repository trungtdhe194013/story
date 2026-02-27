package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GiftRequest {

    @NotNull(message = "Story ID is required")
    private Long storyId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Gift amount must be at least 1 coin")
    private Long amount;
}

