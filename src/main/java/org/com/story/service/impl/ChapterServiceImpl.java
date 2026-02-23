package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.entity.Chapter;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.service.ChapterService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;

    @Override
    public ChapterResponse createChapter(Long storyId, ChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Check ownership
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

        Chapter savedChapter = chapterRepository.save(chapter);
        return mapToResponse(savedChapter, currentUser);
    }

    @Override
    public ChapterResponse getChapter(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // User not logged in
        }

        Story story = chapter.getStory();

        // Check access permission
        boolean isAuthor = currentUser != null && story.getAuthor().getId().equals(currentUser.getId());
        boolean isPublished = "PUBLISHED".equals(chapter.getStatus());
        boolean isFree = chapter.getCoinPrice() == 0;
        boolean isPurchased = currentUser != null && currentUser.getPurchasedChapters().contains(chapter);

        if (!isAuthor && !isPublished) {
            throw new UnauthorizedException("Chapter is not published yet");
        }

        if (!isAuthor && !isFree && !isPurchased) {
            // Return chapter without content
            ChapterResponse response = mapToResponse(chapter, currentUser);
            response.setContent("[Locked] Purchase this chapter to read");
            return response;
        }

        return mapToResponse(chapter, currentUser);
    }

    @Override
    public List<ChapterResponse> getChaptersByStory(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // User not logged in
        }

        boolean isAuthor = currentUser != null && story.getAuthor().getId().equals(currentUser.getId());

        List<Chapter> chapters;
        if (isAuthor) {
            // Author can see all chapters
            chapters = chapterRepository.findByStoryIdOrderByChapterOrderAsc(storyId);
        } else {
            // Others can only see published chapters
            chapters = chapterRepository.findPublishedByStoryId(storyId);
        }

        User finalCurrentUser = currentUser;
        return chapters.stream()
                .map(chapter -> mapToResponse(chapter, finalCurrentUser))
                .collect(Collectors.toList());
    }

    @Override
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Check ownership
        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to update this chapter");
        }

        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setChapterOrder(request.getChapterOrder());
        chapter.setCoinPrice(request.getCoinPrice());
        chapter.setPublishAt(request.getPublishAt());

        Chapter updatedChapter = chapterRepository.save(chapter);
        return mapToResponse(updatedChapter, currentUser);
    }

    @Override
    public void deleteChapter(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Check ownership
        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to delete this chapter");
        }

        chapterRepository.delete(chapter);
    }

    @Override
    public ChapterResponse publishChapter(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Check ownership
        if (!chapter.getStory().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to publish this chapter");
        }

        chapter.setStatus("PUBLISHED");
        if (chapter.getPublishAt() == null) {
            chapter.setPublishAt(LocalDateTime.now());
        }

        Chapter publishedChapter = chapterRepository.save(chapter);
        return mapToResponse(publishedChapter, currentUser);
    }

    @Override
    public ChapterResponse purchaseChapter(Long id) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Check if already purchased
        if (currentUser.getPurchasedChapters().contains(chapter)) {
            throw new BadRequestException("You already purchased this chapter");
        }

        // Check if free
        if (chapter.getCoinPrice() == 0) {
            throw new BadRequestException("This chapter is free");
        }

        // Check wallet balance
        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (wallet.getBalance() < chapter.getCoinPrice()) {
            throw new BadRequestException("Insufficient balance. You need " + chapter.getCoinPrice() + " coins");
        }

        // Deduct coins
        wallet.setBalance(wallet.getBalance() - chapter.getCoinPrice());
        walletRepository.save(wallet);

        // Add chapter to purchased list
        currentUser.getPurchasedChapters().add(chapter);

        // Add coins to author's wallet
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

        return mapToResponse(chapter, currentUser);
    }

    private ChapterResponse mapToResponse(Chapter chapter, User currentUser) {
        boolean isPurchased = currentUser != null &&
                              currentUser.getPurchasedChapters().contains(chapter);

        return ChapterResponse.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .chapterOrder(chapter.getChapterOrder())
                .coinPrice(chapter.getCoinPrice())
                .status(chapter.getStatus())
                .publishAt(chapter.getPublishAt())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .isPurchased(isPurchased)
                .build();
    }
}
