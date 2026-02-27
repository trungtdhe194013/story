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
public class WithdrawRequestResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long amount;
    private String status;
    private LocalDateTime createdAt;
}

