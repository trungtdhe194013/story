package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {
    private Long id;
    private String name;
    private Long rewardCoin;
    private String type;
    private Boolean completed; // trạng thái hoàn thành với user hiện tại
}

