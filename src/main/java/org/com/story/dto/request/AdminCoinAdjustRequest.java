package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCoinAdjustRequest {

    @NotNull(message = "amount không được để trống")
    private Long amount;   // dương = cộng coin, âm = trừ coin

    @NotBlank(message = "reason không được để trống")
    private String reason; // Lý do điều chỉnh (hiển thị trong wallet history)
}

