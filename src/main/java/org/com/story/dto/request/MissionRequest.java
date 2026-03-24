package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MissionRequest {

    @NotBlank(message = "Mission name is required")
    private String name;

    private String description;

    @NotNull(message = "Reward coin is required")
    @Min(value = 1, message = "Reward must be at least 1 coin")
    private Long rewardCoin;

    @NotBlank(message = "Type is required (DAILY, READ)")
    private String type; // DAILY, READ

    /**
     * Hành động kích hoạt nhiệm vụ:
     * LOGIN | READ_CHAPTER | COMMENT | FOLLOW_STORY | BUY_CHAPTER | SEND_GIFT
     */
    @NotBlank(message = "Action is required")
    private String action;

    /** Số lần phải thực hiện để hoàn thành (mặc định 1) */
    @Min(value = 1)
    private Integer targetCount = 1;

    private String icon;

    private Integer displayOrder = 0;

    private Boolean isActive = true;
}

