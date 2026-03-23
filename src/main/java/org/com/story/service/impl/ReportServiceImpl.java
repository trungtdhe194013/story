package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReportRequest;
import org.com.story.dto.request.ResolveReportRequest;
import org.com.story.dto.response.ReportResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.*;
import org.com.story.service.NotificationService;
import org.com.story.service.ReportService;
import org.com.story.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final ChapterPurchaseRepository chapterPurchaseRepository;

    // @Lazy để phá vòng circular dependency (nếu có) — dùng setter injection
    private NotificationService notificationService;

    @Autowired
    public void setNotificationService(@Lazy NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Ngưỡng report để tự động ẩn comment */
    private static final int AUTO_HIDE_COMMENT_THRESHOLD = 5;

    private static final Set<String> VALID_TARGET_TYPES = Set.of("STORY", "CHAPTER", "COMMENT");
    private static final Set<String> VALID_CATEGORIES   = Set.of("SPAM", "COPYRIGHT", "INAPPROPRIATE", "VIOLENCE", "OTHER");
    private static final Set<String> VALID_ACTIONS      = Set.of(
            "WARN_ONLY", "HIDE_CONTENT", "DELETE_CONTENT",
            "BAN_USER", "HIDE_AND_BAN", "DELETE_AND_BAN"
    );

    // ─────────────────────────────────────────────────────────────────────────────
    // TẠO BÁO CÁO
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public ReportResponse createReport(ReportRequest request) {
        User reporter = userService.getCurrentUser();

        // 1. Validate targetType
        String targetType = request.getTargetType().toUpperCase();
        if (!VALID_TARGET_TYPES.contains(targetType)) {
            throw new BadRequestException("targetType phải là STORY, CHAPTER hoặc COMMENT");
        }

        // 2. Validate category
        String category = (request.getCategory() == null || request.getCategory().isBlank())
                ? "OTHER" : request.getCategory().toUpperCase();
        if (!VALID_CATEGORIES.contains(category)) {
            throw new BadRequestException("category phải là: SPAM, COPYRIGHT, INAPPROPRIATE, VIOLENCE, OTHER");
        }

        // 3. Validate target tồn tại & không báo cáo chính mình
        User targetOwner = resolveTarget(targetType, request.getTargetId());
        if (targetOwner != null && targetOwner.getId().equals(reporter.getId())) {
            throw new BadRequestException("Bạn không thể báo cáo nội dung của chính mình");
        }

        // 4. Chống spam báo cáo — mỗi user chỉ báo cáo 1 nội dung 1 lần
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), targetType, request.getTargetId())) {
            throw new BadRequestException("Bạn đã báo cáo nội dung này rồi");
        }

        // 5. Lưu report
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setCategory(category);
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        reportRepository.save(report);

        // 6. Kiểm tra ngưỡng auto-hide cho COMMENT
        if ("COMMENT".equals(targetType)) {
            long distinctReporterCount = reportRepository
                    .countDistinctReportersByTarget(targetType, request.getTargetId());
            if (distinctReporterCount >= AUTO_HIDE_COMMENT_THRESHOLD) {
                commentRepository.findById(request.getTargetId()).ifPresent(comment -> {
                    if (!Boolean.TRUE.equals(comment.getHidden())) {
                        comment.setHidden(true);
                        comment.setHideReason(
                                "Tự động ẩn: nhận " + distinctReporterCount + " báo cáo từ người dùng khác nhau");
                        commentRepository.save(comment);
                        // Thông báo cho admin (thông qua notification system — gửi cho chủ comment)
                        if (targetOwner != null) {
                            try {
                                notificationService.sendNotification(targetOwner, "SYSTEM",
                                        "⚠️ Bình luận của bạn đã bị ẩn tự động",
                                        "Bình luận của bạn đã bị ẩn tạm thời do nhận quá nhiều báo cáo vi phạm từ cộng đồng. Admin sẽ kiểm tra và xử lý.",
                                        request.getTargetId(), "COMMENT");
                            } catch (Exception ignored) {}
                        }
                    }
                });
            }
        }

        return mapToResponse(report);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // XEM BÁO CÁO (USER)
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports() {
        User currentUser = userService.getCurrentUser();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // XEM BÁO CÁO (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getPendingReports() {
        return reportRepository.findByStatus("PENDING")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByTargetType(String targetType) {
        return reportRepository.findByTargetTypeOrderByCreatedAtDesc(targetType.toUpperCase())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsForTarget(String targetType, Long targetId) {
        return reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                        targetType.toUpperCase(), targetId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report #" + id + " không tồn tại"));
        return mapToResponse(report);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // XỬ LÝ BÁO CÁO (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public ReportResponse resolveReport(Long id, ResolveReportRequest request) {
        User admin = userService.getCurrentUser();
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report #" + id + " không tồn tại"));

        if ("RESOLVED".equals(report.getStatus())) {
            throw new BadRequestException("Báo cáo này đã được xử lý rồi");
        }

        String action = request.getAction().toUpperCase();
        if (!VALID_ACTIONS.contains(action)) {
            throw new BadRequestException("action không hợp lệ. Cho phép: " + VALID_ACTIONS);
        }

        boolean shouldHide   = action.equals("HIDE_CONTENT")  || action.equals("HIDE_AND_BAN");
        boolean shouldDelete = action.equals("DELETE_CONTENT") || action.equals("DELETE_AND_BAN");
        boolean shouldBan    = action.equals("BAN_USER")       || action.equals("HIDE_AND_BAN") || action.equals("DELETE_AND_BAN");

        User targetOwner = null;
        String targetTitle = null;
        String effectiveAction = action; // action có thể thay đổi (vd: DELETE bị ép thành HIDE)

        // ── A. XỬ LÝ COMMENT ────────────────────────────────────────────────────
        if ("COMMENT".equals(report.getTargetType())) {
            Comment comment = commentRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new NotFoundException("Comment #" + report.getTargetId() + " không tồn tại"));
            targetOwner = comment.getUser();
            targetTitle = comment.getContent() != null
                    ? comment.getContent().substring(0, Math.min(50, comment.getContent().length())) + "..."
                    : "(comment)";

            if (shouldHide || shouldDelete) {
                String hideReason = request.getAdminNote() != null
                        ? request.getAdminNote() : "Vi phạm quy định cộng đồng";

                if (shouldDelete) {
                    commentRepository.delete(comment);
                    targetTitle = "(comment đã xóa)";
                } else {
                    comment.setHidden(true);
                    comment.setHideReason(hideReason);
                    commentRepository.save(comment);
                }

                // Hạn chế quyền bình luận (24h hoặc 7 ngày tuỳ tái phạm)
                if (targetOwner != null) {
                    applyCommentBan(targetOwner, id, request.getAdminNote());
                }
            }
        }

        // ── B. XỬ LÝ CHAPTER ────────────────────────────────────────────────────
        else if ("CHAPTER".equals(report.getTargetType())) {
            Chapter chapter = chapterRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new NotFoundException("Chapter #" + report.getTargetId() + " không tồn tại"));
            targetOwner = chapter.getStory().getAuthor();
            targetTitle = chapter.getTitle();

            if (shouldHide) {
                chapter.setStatus("HIDDEN");
                chapterRepository.save(chapter);

                // Thông báo tác giả cần chỉnh sửa lại chapter
                if (targetOwner != null) {
                    try {
                        notificationService.sendNotification(targetOwner, "CHAPTER_ACTION",
                                "⚠️ Chương truyện bị ẩn — cần chỉnh sửa",
                                "Chương '" + chapter.getTitle() + "' của truyện '" + chapter.getStory().getTitle()
                                        + "' đã bị ẩn do vi phạm quy định. "
                                        + (request.getAdminNote() != null ? "Lý do: " + request.getAdminNote() + ". " : "")
                                        + "Vui lòng chỉnh sửa lại nội dung và nộp lại để được xem xét.",
                                chapter.getId(), "CHAPTER");
                    } catch (Exception ignored) {}
                }

            } else if (shouldDelete) {
                // Kiểm tra xem chương đã có lượt mua chưa → bảo vệ quyền lợi người dùng
                long purchaseCount = chapterPurchaseRepository.countByChapterId(chapter.getId());
                if (purchaseCount > 0) {
                    // Ép thành HIDE thay vì DELETE để không mất dữ liệu người mua
                    chapter.setStatus("HIDDEN");
                    chapterRepository.save(chapter);
                    effectiveAction = "HIDE_CONTENT";
                    targetTitle = chapter.getTitle() + " (ẩn — không xóa vì có " + purchaseCount + " người đã mua)";

                    if (targetOwner != null) {
                        try {
                            notificationService.sendNotification(targetOwner, "CHAPTER_ACTION",
                                    "⚠️ Chương truyện bị ẩn (không thể xóa)",
                                    "Chương '" + chapter.getTitle() + "' không thể xóa vĩnh viễn vì có "
                                            + purchaseCount + " người đã mua. Chương đã bị ẩn tạm thời. "
                                            + (request.getAdminNote() != null ? "Lý do: " + request.getAdminNote() : ""),
                                    chapter.getId(), "CHAPTER");
                        } catch (Exception ignored) {}
                    }
                } else {
                    chapterRepository.delete(chapter);
                    targetTitle = chapter.getTitle() + " (đã xóa)";
                }
            }
        }

        // ── C. XỬ LÝ STORY ──────────────────────────────────────────────────────
        else if ("STORY".equals(report.getTargetType())) {
            Story story = storyRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new NotFoundException("Story #" + report.getTargetId() + " không tồn tại"));
            targetOwner = story.getAuthor();
            targetTitle = story.getTitle();

            if (shouldHide || shouldDelete) {
                // Luôn dùng soft-delete (không hard-delete story vì ảnh hưởng người mua)
                story.setIsDeleted(true);
                storyRepository.save(story);

                String storyTitle = story.getTitle();

                // Thông báo đến tất cả follower của story
                try {
                    notificationService.sendToFollowers(story.getId(), "STORY_ACTION",
                            "📢 Truyện bạn theo dõi đã bị ẩn",
                            "Truyện '" + storyTitle + "' đã bị ẩn tạm thời do vi phạm quy định. "
                                    + "Xin lỗi vì sự bất tiện này.",
                            story.getId(), "STORY");
                } catch (Exception ignored) {}

                // Thông báo đến những người đã mua chương của story này
                try {
                    List<User> buyers = chapterPurchaseRepository.findDistinctBuyersByStoryId(story.getId());
                    for (User buyer : buyers) {
                        // Tránh gửi 2 lần nếu buyer cũng là follower
                        notificationService.sendNotification(buyer, "STORY_ACTION",
                                "📢 Truyện bạn đã mua đang tạm thời không khả dụng",
                                "Truyện '" + storyTitle + "' mà bạn đã mua chương đang bị ẩn tạm thời do vi phạm quy định. "
                                        + "Lịch sử mua của bạn vẫn được lưu giữ đầy đủ.",
                                story.getId(), "STORY");
                    }
                } catch (Exception ignored) {}

                if (shouldDelete) {
                    targetTitle = storyTitle + " (đã ẩn)";
                }
            }
        }

        // ── BAN TÀI KHOẢN (nếu cần) ─────────────────────────────────────────────
        if (shouldBan && targetOwner != null) {
            if (request.getBanDays() == 0) {
                throw new BadRequestException("banDays phải là -1 (vĩnh viễn) hoặc > 0 khi action có BAN");
            }
            String banReason = "Vi phạm quy định cộng đồng. Báo cáo #" + id
                    + (request.getAdminNote() != null ? ": " + request.getAdminNote() : "");
            targetOwner.setBanReason(banReason);
            if (request.getBanDays() == -1) {
                targetOwner.setBanUntil(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
            } else {
                targetOwner.setBanUntil(LocalDateTime.now().plusDays(request.getBanDays()));
            }
            userRepository.save(targetOwner);
        }

        // ── GỬI THÔNG BÁO CHO TÁC GIẢ NỘI DUNG ─────────────────────────────────
        if (targetOwner != null && !action.equals("WARN_ONLY")) {
            try {
                String notiTitle;
                String notiMsg;
                if (shouldBan) {
                    notiTitle = "⚠️ Tài khoản của bạn bị hạn chế";
                    notiMsg = "Nội dung '" + targetTitle + "' của bạn đã bị xử lý do vi phạm quy định. "
                            + (request.getAdminNote() != null ? "Lý do: " + request.getAdminNote() : "");
                } else if (shouldDelete) {
                    notiTitle = "🗑️ Nội dung đã bị xóa";
                    notiMsg = "Nội dung '" + targetTitle + "' đã bị xóa do vi phạm quy định. "
                            + (request.getAdminNote() != null ? "Lý do: " + request.getAdminNote() : "");
                } else {
                    notiTitle = "🚫 Nội dung đã bị ẩn";
                    notiMsg = "Nội dung '" + targetTitle + "' đã bị ẩn tạm thời do vi phạm quy định. "
                            + (request.getAdminNote() != null ? "Lý do: " + request.getAdminNote() : "");
                }
                notificationService.sendNotification(
                        targetOwner, "SYSTEM", notiTitle, notiMsg,
                        report.getTargetId(), report.getTargetType()
                );
            } catch (Exception ignored) {}
        }

        // ── ĐÁNH DẤU REPORT ĐÃ XỬ LÝ ───────────────────────────────────────────
        report.setStatus("RESOLVED");
        report.setResolvedAction(effectiveAction);
        report.setAdminNote(request.getAdminNote());
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());

        return mapToResponse(reportRepository.save(report));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPERS — COMMENT BAN
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Áp dụng hạn chế bình luận cho user vi phạm:
     * - Lần đầu / lần 2 vi phạm: hạn chế 24 giờ
     * - Từ lần 3 trở đi (tái phạm): hạn chế 7 ngày
     */
    private void applyCommentBan(User user, Long reportId, String adminNote) {
        if (user == null) return;

        // Đếm số comment đã bị ẩn trước đây (không tính comment vừa bị xử lý trong request này)
        long hiddenCount = commentRepository.countByUserIdAndHiddenTrue(user.getId());

        LocalDateTime banUntil;
        String banMsg;
        if (hiddenCount >= 3) {
            // Tái phạm nhiều lần → 7 ngày
            banUntil = LocalDateTime.now().plusDays(7);
            banMsg = "Tái phạm nhiều lần (" + hiddenCount + " lần). Quyền bình luận bị hạn chế 7 ngày.";
        } else if (hiddenCount >= 1) {
            // Tái phạm lần 1-2 → 24h
            banUntil = LocalDateTime.now().plusHours(24);
            banMsg = "Vi phạm lần " + (hiddenCount + 1) + ". Quyền bình luận bị hạn chế 24 giờ.";
        } else {
            // Lần đầu → 24h
            banUntil = LocalDateTime.now().plusHours(24);
            banMsg = "Vi phạm lần đầu. Quyền bình luận bị hạn chế 24 giờ.";
        }

        user.setCommentBanUntil(banUntil);
        userRepository.save(user);

        // Thông báo cho user
        try {
            notificationService.sendNotification(user, "SYSTEM",
                    "🚫 Quyền bình luận bị hạn chế",
                    banMsg + (adminNote != null ? " Lý do: " + adminNote : ""),
                    reportId, "REPORT");
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPERS — RESOLVE TARGET
    // ─────────────────────────────────────────────────────────────────────────────

    private User resolveTarget(String targetType, Long targetId) {
        return switch (targetType) {
            case "STORY" -> {
                Story story = storyRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("Story #" + targetId + " không tồn tại"));
                yield story.getAuthor();
            }
            case "CHAPTER" -> {
                Chapter chapter = chapterRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("Chapter #" + targetId + " không tồn tại"));
                yield chapter.getStory().getAuthor();
            }
            case "COMMENT" -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("Comment #" + targetId + " không tồn tại"));
                yield comment.getUser();
            }
            default -> null;
        };
    }

    private String resolveTargetTitle(String targetType, Long targetId) {
        try {
            return switch (targetType) {
                case "STORY"   -> storyRepository.findById(targetId).map(Story::getTitle).orElse("(đã xóa)");
                case "CHAPTER" -> chapterRepository.findById(targetId).map(Chapter::getTitle).orElse("(đã xóa)");
                case "COMMENT" -> commentRepository.findById(targetId)
                        .map(c -> c.getContent() != null
                                ? c.getContent().substring(0, Math.min(50, c.getContent().length())) + "..."
                                : "(comment)")
                        .orElse("(đã xóa)");
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveTargetOwnerName(String targetType, Long targetId) {
        try {
            return switch (targetType) {
                case "STORY"   -> storyRepository.findById(targetId)
                        .map(s -> s.getAuthor() != null ? s.getAuthor().getFullName() : null).orElse(null);
                case "CHAPTER" -> chapterRepository.findById(targetId)
                        .map(c -> c.getStory() != null && c.getStory().getAuthor() != null
                                ? c.getStory().getAuthor().getFullName() : null).orElse(null);
                case "COMMENT" -> commentRepository.findById(targetId)
                        .map(c -> c.getUser() != null ? c.getUser().getFullName() : null).orElse(null);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(resolveTargetTitle(report.getTargetType(), report.getTargetId()))
                .targetOwnerName(resolveTargetOwnerName(report.getTargetType(), report.getTargetId()))
                .category(report.getCategory())
                .reason(report.getReason())
                .status(report.getStatus())
                .resolvedAction(report.getResolvedAction())
                .adminNote(report.getAdminNote())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByName(report.getResolvedBy() != null ? report.getResolvedBy().getFullName() : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

