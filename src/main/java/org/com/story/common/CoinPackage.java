package org.com.story.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bảng gói nạp coin — càng nạp nhiều tiền càng được nhiều coin hơn
 */
@Getter
@RequiredArgsConstructor
public enum CoinPackage {

    BASIC    (10_000L,  10_000L, "Gói Cơ Bản",    0),
    SAVING   (50_000L,  56_000L, "Gói Tiết Kiệm", 12),
    POPULAR  (100_000L, 118_000L,"Gói Phổ Biến ⭐",18),
    ADVANCED (200_000L, 244_000L,"Gói Nâng Cao",  22),
    VIP      (500_000L, 650_000L,"Gói VIP",        30);

    /** Số tiền VND cần nạp */
    private final Long amountVnd;

    /** Số coin nhận được (đã tính bonus) */
    private final Long coinAmount;

    /** Tên hiển thị */
    private final String displayName;

    /** % bonus so với nạp 1:1 */
    private final int bonusPercent;
}

