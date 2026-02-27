package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MissionRequest {

    @NotBlank(message = "Mission name is required")
    private String name;

    @NotNull(message = "Reward coin is required")
    @Min(value = 1, message = "Reward must be at least 1 coin")
    private Long rewardCoin;

    @NotBlank(message = "Type is required (DAILY, READ)")
    private String type; // DAILY, READ
}

