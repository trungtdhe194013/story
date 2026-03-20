package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaymentLinkRequest {

    /**
     * Tên gói coin muốn nạp: BASIC | SAVING | POPULAR | ADVANCED | VIP
     */
    @NotBlank(message = "packageId is required")
    private String packageId;
}

