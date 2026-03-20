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
public class WalletResponse {
    private Long userId;
    private String userName;
    private Long balance;
    /** Coin đang bị khoá bởi EditRequest đang mở */
    private Long lockedBalance;
    private LocalDateTime updatedAt;
}

