package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse {
    private Long storyId;
    private String storyTitle;
    private String status; // FOLLOWED, UNFOLLOWED
    private String message;
    /** Tổng số follower sau khi toggle */
    private Long followCount;
}

