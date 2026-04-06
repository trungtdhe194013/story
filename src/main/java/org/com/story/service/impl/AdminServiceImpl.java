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
import org.com.story.repository.ChapterPurchaseRepository;
import org.com.story.repository.PaymentOrderRepository;
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
import org.com.story.config.AppLogStore;
import org.springframework.beans.factory.annotation.Value;
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
    private final ChapterPurchaseRepository chapterPurchaseRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final AppLogStore appLogStore;
    private final org.com.story.repository.ReadingHistoryRepository readingHistoryRepository;

    @Value("${app.commission.rate:0.20}")
    private double commissionRate;

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
        // ── Content counts ─────────────────────────────────────────────────────────
        long totalUsers    = userRepository.count();
        long totalStories  = storyRepository.count();
        long totalChapters = chapterRepository.count();
        long totalCats     = categoryRepository.count();
        long totalComments = commentRepository.count();

        // ── Pending queues ─────────────────────────────────────────────────────────
        long pendingStories   = storyRepository.countByStatus("PENDING");
        long pendingChapters  = chapterRepository.countByStatus("PENDING");
        long pendingReports   = reportRepository.countByStatus("PENDING");
        long pendingWithdraws = withdrawRequestRepository.countByStatus("PENDING");

        // ── Real revenue from DB ───────────────────────────────────────────────────
        long totalRevenueVnd    = paymentOrderRepository.sumRevenueVnd();
        long totalPaidOrders    = paymentOrderRepository.countByStatus("PAID");
        long totalCoinSpend     = chapterPurchaseRepository.sumTotalCoinSpend();
        long systemEarningCoin  = chapterPurchaseRepository.sumTotalSystemCommission();
        long totalPurchases     = chapterPurchaseRepository.count();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalStories(totalStories)
                .totalChapters(totalChapters)
                .totalCategories(totalCats)
                .totalComments(totalComments)
                .pendingStories(pendingStories)
                .pendingChapters(pendingChapters)
                .pendingReports(pendingReports)
                .pendingWithdrawRequests(pendingWithdraws)
                .totalRevenueVnd(totalRevenueVnd)
                .totalPaidOrders(totalPaidOrders)
                .totalCoinSpend(totalCoinSpend)
                .systemEarningCoin(systemEarningCoin)
                .totalChapterPurchases(totalPurchases)
                .commissionRate(commissionRate)
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
        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime since7d = now.minusDays(7);
        LocalDateTime todayStart  = LocalDate.now().atStartOfDay();
        LocalDateTime since30d    = now.minusDays(30);

        // ── Users ─────────────────────────────────────────────────────────────────
        long totalUsers = userRepository.count();
        long newUsers7d = userRepository.countByCreatedAtAfter(since7d);

        // ── DAU / MAU (distinct readers) ──────────────────────────────────────────
        long dau = readingHistoryRepository.countDistinctUsersReadingSince(todayStart);
        long mau = readingHistoryRepository.countDistinctUsersReadingSince(since30d);
        double dauMauRatio = mau > 0 ? Math.min(1.0, (double) dau / mau) : 0.0;

        // ── Revenue ───────────────────────────────────────────────────────────────
        long revenueAllTime = paymentOrderRepository.sumRevenueVnd();
        long revenue7d      = paymentOrderRepository.sumRevenueVndSince(since7d);

        // ── Payment error rate (last 7 days) ──────────────────────────────────────
        long paidOrders7d      = paymentOrderRepository.countByStatusSince("PAID",      since7d);
        long cancelledOrders7d = paymentOrderRepository.countByStatusSince("CANCELLED", since7d);
        long totalOrders7d     = paymentOrderRepository.countAllSince(since7d);
        double paymentErrorRate = totalOrders7d > 0
                ? (double) cancelledOrders7d / totalOrders7d
                : 0.0;

        return SystemStatsResponse.builder()
                .dauMauRatio(dauMauRatio)
                .dau(dau)
                .mau(mau)
                .revenue7d(revenue7d)
                .revenueAllTime(revenueAllTime)
                .paymentErrorRate(paymentErrorRate)
                .paidOrders7d(paidOrders7d)
                .cancelledOrders7d(cancelledOrders7d)
                .totalUsers(totalUsers)
                .newUsers7d(newUsers7d)
                .build();
    }

    // ─── [1] Server Logs with filter + pagination ─────────────────────────────

    @Override
    public Page<SystemLogResponse> getSystemLogs(String severity, String component, int page, int size) {
        List<SystemLogResponse> allLogs = appLogStore.getAll(); // real logs from AppLogAppender

        // Filter by severity and component
        List<SystemLogResponse> filtered = allLogs.stream()
                .filter(l -> severity == null || severity.isBlank()
                        || l.getSeverity().equalsIgnoreCase(severity))
                .filter(l -> component == null || component.isBlank()
                        || l.getComponent().toLowerCase().contains(component.toLowerCase()))
                .collect(Collectors.toList());

        // Manual pagination
        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex   = Math.min(fromIndex + size, total);
        List<SystemLogResponse> pageContent = filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }

    // ─── [2] Alerts with severity + acknowledge ───────────────────────────────

    @Override
    public List<SystemAlertResponse> getSystemAlerts() {
        List<SystemAlertResponse> alerts = buildRealAlerts();

        // Merge acknowledged state from in-memory store
        for (SystemAlertResponse alert : alerts) {
            SystemAlertResponse stored = alertStore.get(alert.getId());
            if (stored != null && Boolean.TRUE.equals(stored.getIsAcknowledged())) {
                alert.setIsAcknowledged(true);
                alert.setAcknowledgedAt(stored.getAcknowledgedAt());
                alert.setAcknowledgedBy(stored.getAcknowledgedBy());
            }
        }
        return alerts.stream()
                .sorted((a, b) -> {
                    // Sort by severity (CRITICAL > HIGH > MEDIUM > LOW) then by timestamp desc
                    int sa = severityOrder(a.getSeverity()), sb = severityOrder(b.getSeverity());
                    if (sa != sb) return Integer.compare(sa, sb);
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .collect(Collectors.toList());
    }

    /**
     * Build real alerts from live DB data.
     * Each alert has a DETERMINISTIC id so acknowledged state can be matched across calls.
     */
    private List<SystemAlertResponse> buildRealAlerts() {
        List<SystemAlertResponse> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ── [1] Withdraw requests pending > 72h ──────────────────────────────
        LocalDateTime cutoff72h = now.minusHours(72);
        long pendingOver72h = withdrawRequestRepository.countPendingOlderThan(cutoff72h);
        if (pendingOver72h > 0) {
            alerts.add(SystemAlertResponse.builder()
                    .id("alert-withdraw-72h")
                    .timestamp(now)
                    .level("LOW")
                    .severity("LOW")
                    .message(pendingOver72h + " yeu cau rut tien da PENDING qua 72 gio")
                    .source("WalletService")
                    .isAcknowledged(false)
                    .build());
        }

        // ── [2] Payment error rate — last 5 minutes (CRITICAL threshold) ──────
        LocalDateTime since5min = now.minusMinutes(5);
        long total5  = paymentOrderRepository.countAllSince(since5min);
        long cancelled5 = paymentOrderRepository.countByStatusSince("CANCELLED", since5min);
        if (total5 > 0) {
            double rate5 = (double) cancelled5 / total5 * 100;
            if (rate5 >= 10.0) {
                alerts.add(SystemAlertResponse.builder()
                        .id("alert-payment-5min")
                        .timestamp(now)
                        .level("CRITICAL")
                        .severity("CRITICAL")
                        .message(String.format(
                                "Ti le loi thanh toan %.1f%% trong 5 phut qua (%d/%d don bi huy)",
                                rate5, cancelled5, total5))
                        .source("PaymentGateway")
                        .isAcknowledged(false)
                        .build());
            }
        }

        // ── [3] Payment error rate — last 10 minutes (HIGH threshold) ─────────
        LocalDateTime since10min = now.minusMinutes(10);
        long total10     = paymentOrderRepository.countAllSince(since10min);
        long cancelled10 = paymentOrderRepository.countByStatusSince("CANCELLED", since10min);
        if (total10 > 0) {
            double rate10 = (double) cancelled10 / total10 * 100;
            if (rate10 >= 5.0 && rate10 < 10.0) {
                // Only show HIGH if CRITICAL is not already shown (avoid duplicate)
                boolean criticalAlreadyShown = alerts.stream()
                        .anyMatch(a -> "alert-payment-5min".equals(a.getId()));
                if (!criticalAlreadyShown) {
                    alerts.add(SystemAlertResponse.builder()
                            .id("alert-payment-10min")
                            .timestamp(now)
                            .level("HIGH")
                            .severity("HIGH")
                            .message(String.format(
                                    "Ti le loi thanh toan %.1f%% trong 10 phut qua (%d/%d don bi huy)",
                                    rate10, cancelled10, total10))
                            .source("PaymentGateway")
                            .isAcknowledged(false)
                            .build());
                }
            }
        }

        // ── [4] PENDING payments stale > 30 minutes ───────────────────────────
        LocalDateTime since30min = now.minusMinutes(30);
        long stalePending = paymentOrderRepository.countByStatusSince("PENDING", now.minusYears(1))
                - paymentOrderRepository.countByStatusSince("PENDING", since30min);
        // stalePending = PENDING orders created more than 30 min ago (never completed/cancelled)
        if (stalePending > 0) {
            alerts.add(SystemAlertResponse.builder()
                    .id("alert-payment-stale")
                    .timestamp(now)
                    .level("MEDIUM")
                    .severity("MEDIUM")
                    .message(stalePending + " don thanh toan PENDING qua 30 phut (co the loi gateway)")
                    .source("PaymentGateway")
                    .isAcknowledged(false)
                    .build());
        }

        // ── [5] Disk usage via Java File API ──────────────────────────────────
        try {
            java.io.File disk = new java.io.File("/");
            long total = disk.getTotalSpace();
            if (total > 0) {
                long usable = disk.getUsableSpace();
                double usedPct = (double)(total - usable) / total * 100;
                if (usedPct >= 90.0) {
                    alerts.add(SystemAlertResponse.builder()
                            .id("alert-disk-critical")
                            .timestamp(now)
                            .level("CRITICAL")
                            .severity("CRITICAL")
                            .message(String.format("Disk usage %.1f%% - nguy hiem, can don ngay!", usedPct))
                            .source("DiskMonitor")
                            .isAcknowledged(false)
                            .build());
                } else if (usedPct >= 80.0) {
                    alerts.add(SystemAlertResponse.builder()
                            .id("alert-disk-high")
                            .timestamp(now)
                            .level("HIGH")
                            .severity("HIGH")
                            .message(String.format("Disk usage %.1f%% - sap day, can kiem tra upload files", usedPct))
                            .source("DiskMonitor")
                            .isAcknowledged(false)
                            .build());
                }
                // Under 80%: no disk alert — system healthy
            }
        } catch (Exception ignored) {
            // If disk check fails, skip silently
        }

        return alerts;
    }

    private int severityOrder(String severity) {
        return switch (severity == null ? "" : severity.toUpperCase()) {
            case "CRITICAL" -> 0;
            case "HIGH"     -> 1;
            case "MEDIUM"   -> 2;
            case "LOW"      -> 3;
            default         -> 4;
        };
    }

    @Override
    public SystemAlertResponse acknowledgeAlert(String alertId, String adminEmail) {
        // Rebuild live alerts to get the current state
        List<SystemAlertResponse> current = buildRealAlerts();
        SystemAlertResponse alert = current.stream()
                .filter(a -> alertId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Alert not found or no longer active: " + alertId));

        // Persist acknowledged state in memory store
        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(adminEmail);
        alertStore.put(alertId, alert);
        return alert;
    }

    @Deprecated
    private void ensureDefaultAlerts() { /* replaced by buildRealAlerts() */ }

    @Deprecated
    private void addAlert(String level, String message, String source) { /* replaced by buildRealAlerts() */ }

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
        // Wallet balance
        Long walletBalance = 0L;
        Long lockedBalance = 0L;
        try {
            var walletOpt = walletRepository.findByUserId(user.getId());
            if (walletOpt.isPresent()) {
                walletBalance = walletOpt.get().getBalance();
                lockedBalance = walletOpt.get().getLockedBalance();
            }
        } catch (Exception ignored) {}

        // Stats
        int followedCount = 0;
        try {
            followedCount = user.getFollowedStories() != null ? user.getFollowedStories().size() : 0;
        } catch (Exception ignored) {}

        long purchasedCount = 0;
        try {
            purchasedCount = chapterPurchaseRepository.countByUserId(user.getId());
        } catch (Exception ignored) {}

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(user.getRoles() != null
                        ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                        : java.util.Set.of())
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .enabled(user.getEnabled())
                // Profile
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                // Wallet
                .walletBalance(walletBalance)
                .lockedBalance(lockedBalance)
                // Ban
                .banUntil(user.getBanUntil())
                .banReason(user.getBanReason())
                .commentBanUntil(user.getCommentBanUntil())
                // Stats
                .totalFollowedStories(followedCount)
                .totalPurchasedChapters((int) purchasedCount)
                .totalEarnedCoin(user.getTotalEarnedCoin() != null ? user.getTotalEarnedCoin() : 0L)
                // Timestamps
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
