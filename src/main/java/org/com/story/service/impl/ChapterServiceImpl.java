package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.request.ScheduleChapterRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.CommentResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.*;
import org.com.story.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final WalletRepository walletRepository;
    private final ChapterPurchaseRepository chapterPurchaseRepository;
    private final UserService userService;
    @Lazy private final CommentService commentService;
    @Lazy private final NotificationService notificationService;
    @Lazy private final ReadingHistoryService readingHistoryService;
    private final WalletService walletService;
    @Lazy private final MissionService missionService;

    /** Tỉ lệ hoa hồng hệ thống thu — default 20% */
    @Value("${app.commission.rate:0.20}")
    private double commissionRate;

    @Override
    public ChapterResponse createChapter(Long storyId, ChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        if (!story.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to add chapters to this story");
        }

        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setChapterOrder(request.getChapterOrder());
        chapter.setCoinPrice(request.getCoinPrice());
        chapter.setStatus("DRAFT");
        chapter.setPublishAt(request.getPublishAt());
        chapter.setWordCount(request.getContent() != null ? request.getContent().split("\\s+").length : 0);

        Chapter savedChapter = chapterRepository.save(chapter);
        return mapToResponse(savedChapter, currentUser, Collections.emptyList());
    }

    @Override
    public ChapterResponse getChapter(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        User currentUser = null;
        try { currentUser = userService.getCurrentUser(); } catch (Exception e) {}

        Story story = chapter.getStory();
        boolean isAuthor = currentUser != null && story.getAuthor().getId().equals(currentUser.getId());
        boolean isPublished = "PUBLISHED".equals(chapter.getStatus());
        boolean isFree = chapter.getCoinPrice() == null || chapter.getCoinPrice() == 0;
        boolean isPurchased = currentUser != null &&
                chapterPurchaseRepository.existsByUserIdAndChapterId(currentUser.getId(), chapter.getId());

        if (!isAuthor && !isPublished) {
            throw new UnauthorizedException("Chapter is not published yet");
        }

        if (!isAuthor && !isFree && !isPurchased) {
            ChapterResponse response = mapToResponse(chapter, currentUser, Collections.emptyList());
            response.setContent("[Locked] Purchase this chapter to read");
            return response;
        }

        // Tự động ghi ReadingHistory khi user đọc chapter
        if (currentUser != null && isPublished) {
            try {
                readingHistoryService.recordReading(currentUser.getId(), story.getId(), chapter.getId());
            } catch (Exception ignored) {}

            // Track mission READ_CHAPTER
            try { missionService.trackMissionAction("READ_CHAPTER"); } catch (Exception ignored) {}

            // Tăng viewCount cho Chapter và cho toàn bộ Truyện (Story)
            chapter.setViewCount((chapter.getViewCount() != null ? chapter.getViewCount() : 0) + 1);
            chapterRepository.save(chapter);
            try { storyRepository.incrementViewCount(story.getId()); } catch (Exception ignored) {}
        }

        List<CommentResponse> comments = commentService.getCommentsByChapter(id);
        return mapToResponse(chapter, currentUser, comments);
    }

    @Override
    public List<ChapterResponse> getChaptersByStory(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        User currentUser = null;
        try { currentUser = userService.getCurrentUser(); } catch (Exception e) {}

        boolean isAuthor = currentUser != null && story.getAuthor().getId().equals(currentUser.getId());

        List<Chapter> chapters;
        if (isAuthor) {
            chapters = chapterRepository.findByStoryIdOrderByChapterOrderAsc(storyId);
        } else {
            chapters = chapterRepository.findPublishedByStoryId(storyId);
        }

        User finalCurrentUser = currentUser;
        return chapters.stream()
                .map(chapter -> mapToResponse(chapter, finalCurrentUser, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Override
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to update this chapter");
        }

        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setChapterOrder(request.getChapterOrder());
        chapter.setCoinPrice(request.getCoinPrice());
        chapter.setPublishAt(request.getPublishAt());
        chapter.setWordCount(request.getContent() != null ? request.getContent().split("\\s+").length : 0);

        Chapter updatedChapter = chapterRepository.save(chapter);
        return mapToResponse(updatedChapter, currentUser, Collections.emptyList());
    }

    @Override
    public void deleteChapter(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to delete this chapter");
        }

        chapterRepository.delete(chapter);
    }

    @Override
    public ChapterResponse submitForReview(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to submit this chapter");
        }

        if (!"DRAFT".equals(chapter.getStatus()) && !"EDITED".equals(chapter.getStatus())
                && !"REJECTED".equals(chapter.getStatus())) {
            throw new BadRequestException("Chỉ chapter ở trạng thái DRAFT, EDITED hoặc REJECTED mới có thể nộp duyệt. Hiện tại: " + chapter.getStatus());
        }

        chapter.setStatus("PENDING");
        chapter.setRejectReason(null);
        chapter.setReviewNote(null);
        Chapter updatedChapter = chapterRepository.save(chapter);
        return mapToResponse(updatedChapter, currentUser, Collections.emptyList());
    }

    @Override
    public ChapterResponse publishApprovedChapter(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to publish this chapter");
        }

        if (!"APPROVED".equals(chapter.getStatus())) {
            throw new BadRequestException("Chapter phải được Reviewer APPROVE trước khi publish. Hiện tại: " + chapter.getStatus());
        }

        chapter.setStatus("PUBLISHED");
        chapter.setPublishAt(LocalDateTime.now());
        Chapter updatedChapter = chapterRepository.save(chapter);

        // Gửi notification cho followers
        Story story = chapter.getStory();
        try {
            notificationService.sendToFollowers(
                    story.getId(), "NEW_CHAPTER",
                    "Chương mới: " + chapter.getTitle(),
                    "Truyện '" + story.getTitle() + "' vừa ra chương mới: " + chapter.getTitle(),
                    chapter.getId(), "CHAPTER"
            );
        } catch (Exception ignored) {}

        return mapToResponse(updatedChapter, currentUser, Collections.emptyList());
    }

    @Override
    public ChapterResponse schedulePublish(Long id, ScheduleChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to schedule this chapter");
        }

        if (!"APPROVED".equals(chapter.getStatus())) {
            throw new BadRequestException("Chapter phải được Reviewer APPROVE trước khi hẹn lịch. Hiện tại: " + chapter.getStatus());
        }

        // Kiểm tra thời gian phải trong tương lai (so với giờ server — Asia/Ho_Chi_Minh)
        if (request.getPublishAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("publishAt phải là thời gian trong tương lai (giờ Việt Nam UTC+7). Nhận được: " + request.getPublishAt());
        }

        chapter.setStatus("SCHEDULED");
        chapter.setPublishAt(request.getPublishAt());
        Chapter updated = chapterRepository.save(chapter);
        return mapToResponse(updated, currentUser, Collections.emptyList());
    }

    @Override
    public ChapterResponse purchaseChapter(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (chapterPurchaseRepository.existsByUserIdAndChapterId(currentUser.getId(), chapter.getId())) {
            throw new BadRequestException("You already purchased this chapter");
        }

        if (chapter.getCoinPrice() == null || chapter.getCoinPrice() == 0) {
            throw new BadRequestException("This chapter is free");
        }

        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        long total = chapter.getCoinPrice();

        if (wallet.getBalance() < total) {
            throw new BadRequestException("Insufficient balance. You need " + total + " coins");
        }

        // ── Tính chia hoa hồng ───────────────────────────────────────────────────
        long commission  = Math.round(total * commissionRate);  // 20%
        long authorShare = total - commission;                   // 80%

        // ── Trừ coin người mua ────────────────────────────────────────────────────
        wallet.setBalance(wallet.getBalance() - total);
        walletRepository.save(wallet);

        // ── Lưu ChapterPurchase kèm thông tin hoa hồng ───────────────────────────
        ChapterPurchase purchase = ChapterPurchase.builder()
                .user(currentUser)
                .chapter(chapter)
                .pricePaid((int) total)
                .authorShare(authorShare)
                .commissionCoin(commission)
                .build();
        chapterPurchaseRepository.save(purchase);

        // ── Cộng phần tác giả vào ví ─────────────────────────────────────────────
        User author = chapter.getStory().getAuthor();
        Wallet authorWallet = walletRepository.findByUserId(author.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(author);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
        authorWallet.setBalance(authorWallet.getBalance() + authorShare);
        walletRepository.save(authorWallet);

        // ── Cập nhật tổng coin tác giả đã kiếm (80%) ─────────────────────────────
        author.setTotalEarnedCoin(
                (author.getTotalEarnedCoin() != null ? author.getTotalEarnedCoin() : 0L) + authorShare);

        // ── Ghi 2 giao dịch: Reader trả + Author nhận ────────────────────────────
        walletService.createTransaction(currentUser.getId(), -total,       "BUY",  chapter.getId());
        walletService.createTransaction(author.getId(),      authorShare,  "SELL", chapter.getId());
        // Hoa hồng hệ thống được track qua ChapterPurchase.commissionCoin
        // (không cần system wallet — truy vấn qua SUM(commissionCoin) trong Dashboard)

        // ── Track mission ─────────────────────────────────────────────────────────
        try { missionService.trackMissionAction("BUY_CHAPTER"); } catch (Exception ignored) {}

        // ── Thông báo cho tác giả ─────────────────────────────────────────────────
        try {
            notificationService.sendNotification(
                    author,
                    "NEW_PURCHASE",
                    "Có người mua chương truyện của bạn!",
                    String.format("'%s' đã mua chương '%s' — Bạn nhận được %d xu (sau hoa hồng 20%%).",
                            currentUser.getFullName(), chapter.getTitle(), authorShare),
                    chapter.getId(), "CHAPTER"
            );
        } catch (Exception ignored) {}

        return mapToResponse(chapter, currentUser, Collections.emptyList());
    }

    private ChapterResponse mapToResponse(Chapter chapter, User currentUser, List<CommentResponse> comments) {
        boolean isPurchased = currentUser != null &&
                chapterPurchaseRepository.existsByUserIdAndChapterId(currentUser.getId(), chapter.getId());

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
                .isPurchased(isPurchased)
                .comments(comments)
                .totalComments(comments != null ? comments.size() : 0)
                .build();
    }
}
