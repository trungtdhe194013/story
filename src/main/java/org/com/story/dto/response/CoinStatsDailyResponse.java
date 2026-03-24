package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinStatsDailyResponse {
    // --- existing ---
    private long totalDepositToday;       // Tổng coin nạp hôm nay (qua PayOS)
    private long totalSpendToday;         // Tổng coin tiêu hôm nay (mua chương, quà)

    // --- extended ---
    private long totalWithdrawApprovedToday;  // Coin đã rút được duyệt hôm nay
    private long pendingWithdrawAmount;       // Coin đang chờ rút (chưa duyệt)
    private long pendingWithdrawCount;        // Số lượng yêu cầu rút đang PENDING
    private long totalCoinInCirculation;      // Tổng coin trong ví của TẤT CẢ user (balance + lockedBalance)
    private long coinToVndRate;               // Tỷ giá: 1 coin = ? VND
    private long pendingWithdrawAmountVnd;    // pendingWithdrawAmount * coinToVndRate (nợ nền tảng)
}
