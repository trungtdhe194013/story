package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRoleChangeRequest {

    @NotNull(message = "Request ID is required")
    private Long requestId;

    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    private String action; // APPROVE hoặc REJECT

    private String adminNote; // ghi chú của admin (tùy chọn)
}

