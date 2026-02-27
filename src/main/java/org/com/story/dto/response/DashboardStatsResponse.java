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
    private long totalUsers;
    private long totalStories;
    private long totalChapters;
    private long pendingStories;
    private long pendingReports;
    private long pendingWithdrawRequests;
    private long totalCategories;
    private long totalComments;
}

