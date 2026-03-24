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
    private Long amount;          // số coin muốn rút
    private Long vndAmount;       // số tiền VND admin cần chuyển (amount * tỷ giá)
    private String status;        // PENDING | APPROVED | REJECTED

    // Thông tin ngân hàng
    private String bankName;
    private String bankAccount;
    private String bankOwner;
    private String note;

    // Admin xử lý
    private Long processedById;
    private String processedByName;
    private LocalDateTime processedAt;
    private String rejectedReason;

    private LocalDateTime createdAt;
}
