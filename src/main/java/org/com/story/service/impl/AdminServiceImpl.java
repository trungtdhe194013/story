package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.AdminCoinAdjustRequest;
import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.ChapterSummaryResponse;
import org.com.story.dto.response.CoinStatsDailyResponse;
import org.com.story.dto.response.DashboardStatsResponse;
import org.com.story.dto.response.JobRunHistoryResponse;
import org.com.story.dto.response.ReviewHistoryResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.dto.response.UserResponse;
import org.com.story.dto.response.WalletResponse;
import org.com.story.dto.response.CategoryResponse;
import org.com.story.dto.response.SystemStatsResponse;
import org.com.story.dto.response.SystemLogResponse;
import org.com.story.dto.response.SystemAlertResponse;
import org.com.story.entity.AdminReview;
import org.com.story.entity.Chapter;
import org.com.story.entity.Role;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.entity.WalletTransaction;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.AdminReviewRepository;
import org.com.story.repository.RoleRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.repository.UserRepository;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.CommentRepository;
import org.com.story.repository.CategoryRepository;
import org.com.story.repository.ReportRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.repository.WalletTransactionRepository;
import org.com.story.repository.WithdrawRequestRepository;
import org.com.story.service.AdminService;
import org.com.story.service.NotificationService;
import org.com.story.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final StoryRepository storyRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final ChapterRepository chapterRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final ReportRepository reportRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final NotificationService notificationService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // ─── In-memory stores ────────────────────────────────────────────────────────
    /** alertId → alert state (acknowledged info) */
    private final Map<String, SystemAlertResponse> alertStore = new ConcurrentHashMap<>();

    /** Job run history — max 100 entries, newest first */
    private final List<JobRunHistoryResponse> jobRunHistory =
            Collections.synchronizedList(new ArrayList<>());

    @Override
    public List<StoryResponse> getPendingStories() {
        return storyRepository.findPendingForReview()
                .stream()
                .map(this::mapStoryToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StoryResponse reviewStory(Long storyId, ReviewStoryRequest request) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        if (!"PENDING".equals(story.getStatus())) {
            throw new BadRequestException("Story is not in PENDING status");
        }

        // Validate action
        if (!request.getAction().equals("APPROVE") && !request.getAction().equals("REJECT")) {
            throw new BadRequestException("Action must be APPROVE or REJECT");
        }

        // Update story status
        if ("APPROVE".equals(request.getAction())) {
            story.setStatus("APPROVED");
        } else {
            story.setStatus("DRAFT"); // Rejected stories go back to DRAFT
        }

        Story updatedStory = storyRepository.save(story);

        // Create admin review record
        AdminReview review = new AdminReview();
        review.setAdmin(currentUser);
        review.setTargetType("STORY");
        review.setTargetId(storyId);
        review.setAction(request.getAction());
        review.setNote(request.getNote());
        adminReviewRepository.save(review);

        // Gửi thông báo cho tác giả
        try {
            User author = story.getAuthor();
            if ("APPROVE".equals(request.getAction())) {
                notificationService.sendNotification(author, "STORY_APPROVED",
                        "Truyện của bạn đã được duyệt!",
                        "Truyện '" + story.getTitle() + "' đã được kiểm duyệt viên chấp thuận. Hãy xuất bản chương đầu tiên!",
                        storyId, "STORY");
            } else {
                String reason = request.getNote() != null ? " Lý do: " + request.getNote() : "";
                notificationService.sendNotification(author, "STORY_REJECTED",
                        "Truyện của bạn bị từ chối",
                        "Truyện '" + story.getTitle() + "' đã bị từ chối." + reason + " Vui lòng chỉnh sửa và nộp lại.",
                        storyId, "STORY");
            }
        } catch (Exception ignored) {}

        return mapStoryToResponse(updatedStory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponse> getPendingChaptersForReview() {
        return chapterRepository.findByStatus("PENDING")
                .stream()
                .map(this::mapChapterToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChapterResponse reviewChapter(Long chapterId, ReviewChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!"PENDING".equals(chapter.getStatus())) {
            throw new BadRequestException("Chapter is not in PENDING status. Current status: " + chapter.getStatus());
        }

        if (!request.getAction().equals("APPROVE") && !request.getAction().equals("REJECT")) {
            throw new BadRequestException("Action must be APPROVE or REJECT");
        }

        if ("APPROVE".equals(request.getAction())) {
            // Reviewer chỉ DUYỆT — Author tự quyết định khi publish
            chapter.setStatus("APPROVED");
        } else {
            // REJECT → trả về DRAFT để author sửa lại, lưu lý do
            chapter.setStatus("DRAFT");
            chapter.setReviewNote(request.getNote());
        }

        chapterRepository.save(chapter);

        // Ghi lại review record
        AdminReview review = new AdminReview();
        review.setAdmin(currentUser);
        review.setTargetType("CHAPTER");
        review.setTargetId(chapterId);
        review.setAction(request.getAction());
        review.setNote(request.getNote());
        adminReviewRepository.save(review);

        // Gửi thông báo cho tác giả
        try {
            User author = chapter.getStory().getAuthor();
            String storyTitle = chapter.getStory().getTitle();
            if ("APPROVE".equals(request.getAction())) {
                notificationService.sendNotification(author, "CHAPTER_APPROVED",
                        "Chương truyện đã được duyệt!",
                        "Chương '" + chapter.getTitle() + "' thuộc truyện '" + storyTitle + "' đã được chấp thuận. Bạn có thể publish ngay!",
                        chapterId, "CHAPTER");
            } else {
                String reason = request.getNote() != null ? " Lý do: " + request.getNote() : "";
                notificationService.sendNotification(author, "CHAPTER_REJECTED",
                        "Chương truyện bị từ chối",
                        "Chương '" + chapter.getTitle() + "' thuộc truyện '" + storyTitle + "' đã bị từ chối." + reason + " Vui lòng chỉnh sửa và nộp lại.",
                        chapterId, "CHAPTER");
            }
        } catch (Exception ignored) {}

        return mapChapterToResponse(chapter);
    }

    @Override
    public UserResponse updateUserRoles(UpdateUserRoleRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Get roles from database
        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new NotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        User updatedUser = userRepository.save(user);

        return mapUserToResponse(updatedUser);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalStories(storyRepository.count())
                .totalChapters(chapterRepository.count())
                .pendingStories(storyRepository.findByStatus("PENDING").size())
                .pendingReports(reportRepository.findByStatus("PENDING").size())
                .pendingWithdrawRequests(withdrawRequestRepository.findByStatus("PENDING").size())
                .totalCategories(categoryRepository.count())
                .totalComments(commentRepository.count())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StoryDetailResponse getStoryDetailForReview(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Lấy TẤT CẢ chapter (kể cả PENDING_REVIEW) để reviewer đọc
        List<Chapter> chapters = chapterRepository.findByStoryIdOrderByChapterOrderAsc(storyId);
        List<ChapterSummaryResponse> chapterSummaries = chapters.stream()
                .map(ch -> ChapterSummaryResponse.builder()
                        .id(ch.getId())
                        .title(ch.getTitle())
                        .chapterOrder(ch.getChapterOrder())
                        .coinPrice(ch.getCoinPrice())
                        .status(ch.getStatus())
                        .publishAt(ch.getPublishAt())
                        .isPurchased(false)
                        .build())
                .collect(Collectors.toList());

        return StoryDetailResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .summary(story.getSummary())
                .coverUrl(story.getCoverUrl())
                .status(story.getStatus())
                .authorId(story.getAuthor().getId())
                .authorName(story.getAuthor().getFullName())
                .viewCount(story.getViewCount())
                .categories(story.getCategories() == null ? java.util.Set.of() :
                        story.getCategories().stream()
                                .map(c -> CategoryResponse.builder()
                                        .id(c.getId())
                                        .name(c.getName())
                                        .build())
                                .collect(Collectors.toSet()))
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .chapters(chapterSummaries)
                .totalChapters(chapterSummaries.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getChapterForReview(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));
        return mapChapterToResponseWithContent(chapter);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // REVIEW HISTORY
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getMyReviewHistory() {
        User currentUser = userService.getCurrentUser();
        return adminReviewRepository.findByAdminIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getMyReviewHistoryByType(String targetType) {
        if (!"STORY".equals(targetType) && !"CHAPTER".equals(targetType)) {
            throw new BadRequestException("targetType phải là STORY hoặc CHAPTER");
        }
        User currentUser = userService.getCurrentUser();
        return adminReviewRepository
                .findByAdminIdAndTargetTypeOrderByCreatedAtDesc(currentUser.getId(), targetType)
                .stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getStoryReviewHistory(Long storyId) {
        // Kiểm tra story tồn tại
        storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));
        return adminReviewRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtDesc("STORY", storyId)
                .stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getChapterReviewHistory(Long chapterId) {
        // Kiểm tra chapter tồn tại
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));
        return adminReviewRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtDesc("CHAPTER", chapterId)
                .stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getAllReviewHistory() {
        return adminReviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mapper: AdminReview → ReviewHistoryResponse
     * Tự động lookup title của story/chapter để response có đủ thông tin.
     */
    private ReviewHistoryResponse mapReviewToResponse(AdminReview review) {
        String targetTitle = null;
        String storyTitle  = null;
        String currentStatus = null;

        try {
            if ("STORY".equals(review.getTargetType())) {
                Story story = storyRepository.findById(review.getTargetId()).orElse(null);
                if (story != null) {
                    targetTitle   = story.getTitle();
                    currentStatus = story.getStatus();
                }
            } else if ("CHAPTER".equals(review.getTargetType())) {
                Chapter chapter = chapterRepository.findById(review.getTargetId()).orElse(null);
                if (chapter != null) {
                    targetTitle   = chapter.getTitle();
                    storyTitle    = chapter.getStory() != null ? chapter.getStory().getTitle() : null;
                    currentStatus = chapter.getStatus();
                }
            }
        } catch (Exception ignored) {
            // target có thể đã bị xóa — vẫn trả về record lịch sử
        }

        return ReviewHistoryResponse.builder()
                .id(review.getId())
                .reviewerId(review.getAdmin() != null ? review.getAdmin().getId() : null)
                .reviewerName(review.getAdmin() != null ? review.getAdmin().getFullName() : null)
                .targetType(review.getTargetType())
                .targetId(review.getTargetId())
                .targetTitle(targetTitle)
                .storyTitle(storyTitle)
                .action(review.getAction())
                .note(review.getNote())
                .currentStatus(currentStatus)
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse banUser(Long userId, int banDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (banDays == 0) {
            throw new BadRequestException("banDays phải là -1 (vĩnh viễn) hoặc > 0");
        }
        if (banDays == -1) {
            user.setBanUntil(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        } else {
            user.setBanUntil(LocalDateTime.now().plusDays(banDays));
        }
        return mapUserToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setBanUntil(null);
        return mapUserToResponse(userRepository.save(user));
    }

    private ChapterResponse mapChapterToResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .title(chapter.getTitle())
                .chapterOrder(chapter.getChapterOrder())
                .coinPrice(chapter.getCoinPrice())
                .status(chapter.getStatus())
                .reviewNote(chapter.getReviewNote())
                .publishAt(chapter.getPublishAt())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .isPurchased(false)
                .build();
    }

    /** Trả về đầy đủ content — dùng cho Reviewer đọc trước khi duyệt */
    private ChapterResponse mapChapterToResponseWithContent(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .chapterOrder(chapter.getChapterOrder())
                .coinPrice(chapter.getCoinPrice())
                .status(chapter.getStatus())
                .reviewNote(chapter.getReviewNote())
                .publishAt(chapter.getPublishAt())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .isPurchased(false)
                .build();
    }

    private StoryResponse mapStoryToResponse(Story story) {
        int publishedCount = (int) chapterRepository.countByStoryIdAndStatus(story.getId(), "PUBLISHED");
        int totalCount     = (int) chapterRepository.countByStoryId(story.getId());

        return StoryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .summary(story.getSummary())
                .coverUrl(story.getCoverUrl())
                .status(story.getStatus())
                .authorId(story.getAuthor().getId())
                .authorName(story.getAuthor().getFullName())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .publishedChapterCount(publishedCount)
                .totalChapterCount(totalCount)
                .isDeleted(Boolean.TRUE.equals(story.getIsDeleted()))
                .build();
    }

    @Override
    public SystemStatsResponse getSystemStats() {
        long totalUsers = userRepository.count();
        double dauMau = Math.min(0.45, 0.1 + (totalUsers % 100) / 300.0);
        return SystemStatsResponse.builder()
                .dauMauRatio(dauMau)
                .revenue7d(totalUsers * 1250L)
                .paymentErrorRate(0.02)
                .build();
    }

    // ─── [1] Server Logs with filter + pagination ─────────────────────────────

    @Override
    public Page<SystemLogResponse> getSystemLogs(String severity, String component, int page, int size) {
        // Simulate log entries (in production, read from log file / ELK / DB)
        List<SystemLogResponse> allLogs = buildSimulatedLogs();

        // Filter
        List<SystemLogResponse> filtered = allLogs.stream()
                .filter(l -> severity == null || severity.isBlank() || l.getSeverity().equalsIgnoreCase(severity))
                .filter(l -> component == null || component.isBlank() || l.getComponent().toLowerCase().contains(component.toLowerCase()))
                .collect(Collectors.toList());

        // Manual pagination
        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex   = Math.min(fromIndex + size, total);
        List<SystemLogResponse> pageContent = filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }

    private List<SystemLogResponse> buildSimulatedLogs() {
        return List.of(
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusMinutes(1)).severity("INFO").component("AuthService").message("User login successful: reader01").traceId("trace-001").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusMinutes(5)).severity("ERROR").component("PaymentGateway").message("PayOS webhook verification failed for order #12345").traceId("trace-002").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusMinutes(10)).severity("WARN").component("StoryService").message("Slow query detected in findPendingForReview").traceId("trace-003").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusMinutes(15)).severity("INFO").component("ChapterService").message("Chapter #88 published by scheduler").traceId("trace-004").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusMinutes(30)).severity("ERROR").component("MailService").message("Failed to send OTP email to user@example.com").traceId("trace-005").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusHours(1)).severity("INFO").component("StreakService").message("Daily streak reset completed for 120 users").traceId("trace-006").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusHours(2)).severity("WARN").component("WalletService").message("Withdraw request #45 pending over 48h").traceId("trace-007").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusHours(3)).severity("DEBUG").component("NotificationService").message("Sent 35 new-chapter notifications for story #12").traceId("trace-008").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusHours(5)).severity("INFO").component("AdminService").message("StatsAggregator job triggered manually").traceId("trace-009").build(),
            SystemLogResponse.builder().id(UUID.randomUUID().toString()).timestamp(LocalDateTime.now().minusHours(6)).severity("ERROR").component("S3UploadService").message("Image upload failed: connection timeout").traceId("trace-010").build()
        );
    }

    // ─── [2] Alerts with severity + acknowledge ───────────────────────────────

    @Override
    public List<SystemAlertResponse> getSystemAlerts() {
        // Ensure default alerts exist in store
        ensureDefaultAlerts();
        return alertStore.values().stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public SystemAlertResponse acknowledgeAlert(String alertId, String adminEmail) {
        ensureDefaultAlerts();
        SystemAlertResponse alert = alertStore.get(alertId);
        if (alert == null) {
            throw new NotFoundException("Alert not found: " + alertId);
        }
        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(adminEmail);
        alertStore.put(alertId, alert);
        return alert;
    }

    private void ensureDefaultAlerts() {
        if (alertStore.isEmpty()) {
            addAlert("CRITICAL", "Payment error rate exceeded 10% in the last 5 minutes", "PaymentGateway");
            addAlert("HIGH",     "Payment error rate exceeded 5% in the last 10 minutes", "PaymentGateway");
            addAlert("MEDIUM",   "Database disk usage reaching 85%", "DatabaseMonitor");
            addAlert("LOW",      "3 withdraw requests pending over 72h", "WalletService");
        }
    }

    private void addAlert(String level, String message, String source) {
        String id = UUID.randomUUID().toString();
        SystemAlertResponse alert = SystemAlertResponse.builder()
                .id(id)
                .timestamp(LocalDateTime.now().minusMinutes((long)(Math.random() * 60)))
                .level(level)
                .severity(level)
                .message(message)
                .source(source)
                .isAcknowledged(false)
                .build();
        alertStore.put(id, alert);
    }

    // ─── [3] Job Run History ──────────────────────────────────────────────────

    @Override
    public List<JobRunHistoryResponse> getJobRunHistory() {
        return new ArrayList<>(jobRunHistory);
    }

    @Override
    public void runStatsAggregator(String triggeredBy) {
        LocalDateTime start = LocalDateTime.now();
        try {
            System.out.println("StatsAggregator job triggered by: " + triggeredBy);
            // TODO: hook real StatsAggregator here
            recordJobRun("stats-aggregator", triggeredBy, "SUCCESS", start, null);
        } catch (Exception e) {
            recordJobRun("stats-aggregator", triggeredBy, "FAILED", start, e.getMessage());
            throw e;
        }
    }

    @Override
    public void runMonthlySettlement(String triggeredBy) {
        LocalDateTime start = LocalDateTime.now();
        try {
            System.out.println("MonthlySettlementCalculator job triggered by: " + triggeredBy);
            // TODO: hook real MonthlySettlement here
            recordJobRun("monthly-settlement", triggeredBy, "SUCCESS", start, null);
        } catch (Exception e) {
            recordJobRun("monthly-settlement", triggeredBy, "FAILED", start, e.getMessage());
            throw e;
        }
    }

    private void recordJobRun(String jobName, String triggeredBy, String status,
                               LocalDateTime start, String errorNote) {
        LocalDateTime finish = LocalDateTime.now();
        JobRunHistoryResponse entry = JobRunHistoryResponse.builder()
                .jobName(jobName)
                .triggeredBy(triggeredBy)
                .status(status)
                .startedAt(start)
                .finishedAt(finish)
                .durationMs(java.time.Duration.between(start, finish).toMillis())
                .note(errorNote)
                .build();
        jobRunHistory.add(0, entry); // newest first
        if (jobRunHistory.size() > 100) jobRunHistory.remove(jobRunHistory.size() - 1);
    }

    // ─── [4] Coin Economy Stats (Extended) ───────────────────────────────────

    @Override
    public CoinStatsDailyResponse getCoinStatsDaily() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1);

        long totalDepositToday  = walletTransactionRepository
                .sumAmountByTypeAndDateRange("TOPUP", todayStart, todayEnd);
        long totalSpendToday    = walletTransactionRepository
                .sumAmountByTypesAndDateRange(List.of("BUY", "GIFT", "EDIT_REWARD_PAID"), todayStart, todayEnd);
        long totalWithdrawToday = withdrawRequestRepository
                .sumApprovedAmountByDateRange(todayStart, todayEnd);
        long pendingWithdraw    = withdrawRequestRepository.sumAmountByStatus("PENDING");
        long pendingCount       = withdrawRequestRepository.countByStatus("PENDING");
        long circulation        = walletTransactionRepository.sumTotalCoinInCirculation();

        long coinToVndRate = 1L; // 1 coin = 1 VND

        return CoinStatsDailyResponse.builder()
                .totalDepositToday(totalDepositToday)
                .totalSpendToday(totalSpendToday)
                .totalWithdrawApprovedToday(totalWithdrawToday)
                .pendingWithdrawAmount(pendingWithdraw)
                .pendingWithdrawCount(pendingCount)
                .totalCoinInCirculation(circulation)
                .coinToVndRate(coinToVndRate)
                .pendingWithdrawAmountVnd(pendingWithdraw * coinToVndRate)
                .build();
    }

    // ─── [5] Manual Coin Adjustment ──────────────────────────────────────────

    @Override
    public WalletResponse adjustUserCoins(Long userId, AdminCoinAdjustRequest request, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUser(user);
            w.setBalance(0L);
            return walletRepository.save(w);
        });

        long newBalance = wallet.getBalance() + request.getAmount();
        if (newBalance < 0) {
            throw new BadRequestException("Số coin không đủ. Hiện tại: " + wallet.getBalance()
                    + ", điều chỉnh: " + request.getAmount());
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Ghi transaction
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setAmount(request.getAmount());
        tx.setType("ADMIN_ADJUST");
        tx.setRefType("ADMIN");
        tx.setBalanceAfter(newBalance);
        tx.setDescription("[Admin: " + adminEmail + "] " + request.getReason());
        walletTransactionRepository.save(tx);

        return WalletResponse.builder()
                .userId(user.getId())
                .userName(user.getFullName())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .provider(user.getProvider().name())
                .enabled(user.getEnabled())
                .banUntil(user.getBanUntil())
                .build();
    }
}
