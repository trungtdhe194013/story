package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // ── Tổng quan nội dung ─────────────────────────────────────────────────────
    private long totalUsers;
    private long totalStories;
    private long totalChapters;
    private long totalCategories;
    private long totalComments;

    // ── Chờ xử lý ─────────────────────────────────────────────────────────────
    private long pendingStories;
    private long pendingChapters;
    private long pendingReports;
    private long pendingWithdrawRequests;

    // ── Doanh thu thực từ DB ───────────────────────────────────────────────────
    /** Tổng tiền VND đã thu từ PayOS (status = PAID) */
    private long totalRevenueVnd;

    /** Số đơn nạp coin thành công */
    private long totalPaidOrders;

    /** Tổng coin đã tiêu qua mua chương */
    private long totalCoinSpend;

    /** Tổng coin hoa hồng hệ thống đã thu (20% mỗi giao dịch mua chương) */
    private long systemEarningCoin;

    /** Tổng số giao dịch mua chương */
    private long totalChapterPurchases;

    /** Tỉ lệ hoa hồng hiện tại (ví dụ: 0.20 = 20%) */
    private double commissionRate;
}

