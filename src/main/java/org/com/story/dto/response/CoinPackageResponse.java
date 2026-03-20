package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response mô tả một gói coin (dùng cho trang chọn gói) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinPackageResponse {

    private String id;           // enum name: BASIC, SAVING, ...
    private String displayName;
    private Long amountVnd;
    private Long coinAmount;
    private int bonusPercent;
}

