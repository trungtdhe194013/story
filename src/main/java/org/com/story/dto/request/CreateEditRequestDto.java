package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateEditRequestDto {

    @NotNull(message = "chapterId là bắt buộc")
    private Long chapterId;

    @NotNull(message = "coinReward là bắt buộc")
    @Min(value = 1, message = "coinReward phải >= 1")
    private Long coinReward;

    @NotBlank(message = "description là bắt buộc")
    private String description;
}

