package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.CommentResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.*;
import org.com.story.service.*;
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
    @Lazy
    private final CommentService commentService;
    @Lazy
    private final NotificationService notificationService;
    @Lazy
    private final ReadingHistoryService readingHistoryService;
    private final WalletService walletService;
    private final ExperienceService experienceService;

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

            // Award EXP
            experienceService.awardExperience(currentUser, ExperienceService.EXP_PER_CHAPTER_READ);

            // Increase viewCount
            chapter.setViewCount((chapter.getViewCount() != null ? chapter.getViewCount() : 0) + 1);
            chapterRepository.save(chapter);
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

        if (wallet.getBalance() < chapter.getCoinPrice()) {
            throw new BadRequestException("Insufficient balance. You need " + chapter.getCoinPrice() + " coins");
        }

        // Trừ tiền người mua
        wallet.setBalance(wallet.getBalance() - chapter.getCoinPrice());
        walletRepository.save(wallet);

        // Lưu ChapterPurchase
        ChapterPurchase purchase = ChapterPurchase.builder()
                .user(currentUser)
                .chapter(chapter)
                .pricePaid(chapter.getCoinPrice())
                .build();
        chapterPurchaseRepository.save(purchase);

        // Cộng tiền cho tác giả
        User author = chapter.getStory().getAuthor();
        Wallet authorWallet = walletRepository.findByUserId(author.getId())
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(author);
                    newWallet.setBalance(0L);
                    return walletRepository.save(newWallet);
                });
        authorWallet.setBalance(authorWallet.getBalance() + chapter.getCoinPrice());
        walletRepository.save(authorWallet);

        // Update totalEarnedCoin
        author.setTotalEarnedCoin((author.getTotalEarnedCoin() != null ? author.getTotalEarnedCoin() : 0L) + chapter.getCoinPrice());

        // Ghi giao dịch
        walletService.createTransaction(author.getId(), (long) chapter.getCoinPrice(), "BUY", chapter.getId());

        // Award EXP for buying (maybe double?)
        experienceService.awardExperience(currentUser, ExperienceService.EXP_PER_CHAPTER_READ * 2);

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
