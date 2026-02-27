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
public class WalletTransactionResponse {
    private Long id;
    private Long userId;
    private Long amount;
    private String type; // TOPUP, BUY, GIFT, REWARD
    private Long refId;
    private LocalDateTime createdAt;
}

