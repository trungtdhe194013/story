package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawRequestDto {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Withdraw amount must be at least 1")
    private Long amount;
}

