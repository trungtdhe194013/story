package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {

    private Long id;
    private Long orderCode;
    private String packageId;
    private String packageName;
    private Long amountVnd;
    private Long coinAmount;
    private String status;      // PENDING | PAID | CANCELLED
    private String checkoutUrl;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}

