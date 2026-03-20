package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {

    private Long orderCode;
    private String packageId;
    private String packageName;
    private Long amountVnd;
    private Long coinAmount;
    private int bonusPercent;

    /** URL trang thanh toán PayOS — frontend redirect sang đây */
    private String checkoutUrl;
}

