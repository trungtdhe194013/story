package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsResponse {
    /** DAU/MAU ratio (0.0 - 1.0) */
    private double dauMauRatio;

    /** DAU: distinct readers today */
    private long dau;

    /** MAU: distinct readers in last 30 days */
    private long mau;

    /** Doanh thu 7 ngay gan nhat (VND) */
    private long revenue7d;

    /** Tong doanh thu tu truoc den nay (VND). Source: PaymentOrder PAID */
    private long revenueAllTime;

    /** Ti le don thanh toan bi huy trong 7 ngay (0.0 - 1.0). Source: PaymentOrder CANCELLED */
    private double paymentErrorRate;

    /** So don PAID trong 7 ngay */
    private long paidOrders7d;

    /** So don CANCELLED trong 7 ngay */
    private long cancelledOrders7d;

    /** Tong so nguoi dung */
    private long totalUsers;

    /** So nguoi dung moi trong 7 ngay */
    private long newUsers7d;
}
