package org.com.story.service;

import org.com.story.dto.request.AdminCoinAdjustRequest;
import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.CoinStatsDailyResponse;
import org.com.story.dto.response.DashboardStatsResponse;
import org.com.story.dto.response.JobRunHistoryResponse;
import org.com.story.dto.response.ReviewHistoryResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.dto.response.SystemAlertResponse;
import org.com.story.dto.response.SystemLogResponse;
import org.com.story.dto.response.SystemStatsResponse;
import org.com.story.dto.response.UserResponse;
import org.com.story.dto.response.WalletResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminService {
    List<StoryResponse> getPendingStories();
    StoryResponse reviewStory(Long storyId, ReviewStoryRequest request);

    // Chapter review
    List<ChapterResponse> getPendingChaptersForReview();
    ChapterResponse reviewChapter(Long chapterId, ReviewChapterRequest request);

    // Reviewer đọc nội dung đầy đủ trước khi duyệt
    StoryDetailResponse getStoryDetailForReview(Long storyId);
    ChapterResponse getChapterForReview(Long chapterId);

    // ─── Lịch sử duyệt ───────────────────────────────────────────────────────────

    /** Lịch sử duyệt của reviewer đang đăng nhập (tất cả STORY + CHAPTER) */
    List<ReviewHistoryResponse> getMyReviewHistory();

    /** Lịch sử duyệt của reviewer đang đăng nhập, lọc theo loại: STORY hoặc CHAPTER */
    List<ReviewHistoryResponse> getMyReviewHistoryByType(String targetType);

    /** Lịch sử duyệt của 1 story cụ thể (ai duyệt, bao giờ, kết quả gì) */
    List<ReviewHistoryResponse> getStoryReviewHistory(Long storyId);

    /** Lịch sử duyệt của 1 chapter cụ thể */
    List<ReviewHistoryResponse> getChapterReviewHistory(Long chapterId);

    /** [Admin] Xem toàn bộ lịch sử duyệt của hệ thống */
    List<ReviewHistoryResponse> getAllReviewHistory();

    // ─── User management ─────────────────────────────────────────────────────────
    UserResponse updateUserRoles(UpdateUserRoleRequest request);
    List<UserResponse> getAllUsers();

    DashboardStatsResponse getDashboardStats();

    /** Ban tài khoản. banDays = -1 là ban vĩnh viễn, banDays > 0 là số ngày */
    UserResponse banUser(Long userId, int banDays);

    /** Gỡ ban tài khoản */
    UserResponse unbanUser(Long userId);

    // ─── System Ops & Coin Monitoring ───────────────────────────────────────────

    SystemStatsResponse getSystemStats();

    /** [1] Server logs với filter + pagination */
    Page<SystemLogResponse> getSystemLogs(String severity, String component, int page, int size);

    /** [2] Alerts với severity field */
    List<SystemAlertResponse> getSystemAlerts();

    /** [2b] Đánh dấu alert đã xử lý */
    SystemAlertResponse acknowledgeAlert(String alertId, String adminEmail);

    /** [3] Job run history */
    List<JobRunHistoryResponse> getJobRunHistory();

    void runStatsAggregator(String triggeredBy);

    /** [4] Coin economy stats mở rộng */
    CoinStatsDailyResponse getCoinStatsDaily();

    void runMonthlySettlement(String triggeredBy);

    /** [5] Điều chỉnh coin thủ công */
    WalletResponse adjustUserCoins(Long userId, AdminCoinAdjustRequest request, String adminEmail);
}


