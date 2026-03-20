package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.*;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.CategoryRepository;
import org.com.story.repository.ChapterPurchaseRepository;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.RatingRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.repository.UserRepository;
import org.com.story.service.StoryService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterPurchaseRepository chapterPurchaseRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // Helper: resolve Category entities from IDs
    // ─────────────────────────────────────────────
    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return new HashSet<>();
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
        if (categories.size() != categoryIds.size()) {
            Set<Long> foundIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
            Set<Long> missing = categoryIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new NotFoundException("Category IDs not found: " + missing);
        }
        return categories;
    }

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────
    @Override
    public StoryResponse createStory(StoryRequest request) {
        User currentUser = userService.getCurrentUser();

        Story story = new Story();
        story.setAuthor(currentUser);
        story.setTitle(request.getTitle());
        story.setSummary(request.getSummary());
        story.setCoverUrl(request.getCoverUrl());
        story.setStatus("DRAFT");
        story.setIsDeleted(false);

        // Gắn thể loại nếu có
        story.setCategories(resolveCategories(request.getCategoryIds()));

        return mapToResponse(storyRepository.save(story));
    }

    // ─────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────
    @Override
    public StoryResponse getStory(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        User currentUser = null;
        try { currentUser = userService.getCurrentUser(); } catch (Exception ignored) {}

        boolean isOwner = currentUser != null && story.getAuthor().getId().equals(currentUser.getId());

        // Nếu bị soft-delete hoặc chưa APPROVED thì chỉ author mới xem được
        if (Boolean.TRUE.equals(story.getIsDeleted()) || !"APPROVED".equals(story.getStatus())) {
            if (!isOwner) throw new UnauthorizedException("You don't have permission to view this story");
        }

        // Tăng viewCount
        if (!isOwner) {
            story.setViewCount((story.getViewCount() != null ? story.getViewCount() : 0L) + 1);
            storyRepository.save(story);
        }

        return mapToResponse(story);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryDetailResponse getStoryDetail(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        User currentUser = null;
        try { currentUser = userService.getCurrentUser(); } catch (Exception ignored) {}
        final User finalCurrentUser = currentUser;

        boolean isOwner = finalCurrentUser != null && story.getAuthor().getId().equals(finalCurrentUser.getId());
        if (Boolean.TRUE.equals(story.getIsDeleted()) || !"APPROVED".equals(story.getStatus())) {
            if (!isOwner) throw new UnauthorizedException("You don't have permission to view this story");
        }

        // Danh sách chương PUBLISHED (tóm tắt, không có content)
        List<Chapter> publishedChapters = chapterRepository.findPublishedByStoryId(id);
        List<ChapterSummaryResponse> chapterSummaries = publishedChapters.stream()
                .map(ch -> ChapterSummaryResponse.builder()
                        .id(ch.getId())
                        .title(ch.getTitle())
                        .chapterOrder(ch.getChapterOrder())
                        .coinPrice(ch.getCoinPrice())
                        .status(ch.getStatus())
                        .publishAt(ch.getPublishAt())
                        .isPurchased(finalCurrentUser != null &&
                                chapterPurchaseRepository.existsByUserIdAndChapterId(finalCurrentUser.getId(), ch.getId()))
                        .build())
                .collect(Collectors.toList());

        long allChapters = chapterRepository.countByStoryId(id);

        // Lấy rating của user hiện tại (nếu đã đăng nhập)
        Integer myRating = null;
        String myReview = null;
        if (finalCurrentUser != null) {
            var myRatingOpt = ratingRepository.findByUserIdAndStoryId(finalCurrentUser.getId(), id);
            if (myRatingOpt.isPresent()) {
                myRating = myRatingOpt.get().getScore();
                myReview = myRatingOpt.get().getReview();
            }
        }

        long followCount = userRepository.countFollowersByStoryId(id);
        Boolean isFollowing = finalCurrentUser == null ? null :
                finalCurrentUser.getFollowedStories().stream().anyMatch(s -> s.getId().equals(id));

        return StoryDetailResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .summary(story.getSummary())
                .coverUrl(story.getCoverUrl())
                .status(story.getStatus())
                .authorId(story.getAuthor().getId())
                .authorName(story.getAuthor().getFullName())
                .viewCount(story.getViewCount() != null ? story.getViewCount() : 0L)
                .avgRating(story.getAvgRating() != null ? story.getAvgRating() : 0.0)
                .ratingCount(story.getRatingCount() != null ? story.getRatingCount() : 0)
                .isCompleted(Boolean.TRUE.equals(story.getIsCompleted()))
                .myRating(myRating)
                .myReview(myReview)
                .categories(story.getCategories() == null ? Set.of() :
                        story.getCategories().stream()
                                .map(c -> CategoryResponse.builder().id(c.getId()).name(c.getName()).build())
                                .collect(Collectors.toSet()))
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .chapters(chapterSummaries)
                .totalChapters(chapterSummaries.size())
                .allChaptersCount((int) allChapters)
                .followCount(followCount)
                .isFollowing(isFollowing)
                .build();
    }

    @Override
    public List<StoryResponse> getAllPublishedStories() {
        // APPROVED + isDeleted=false + ít nhất 1 chapter PUBLISHED
        return storyRepository.findAllPublished().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StoryResponse> getMyStories() {
        User currentUser = userService.getCurrentUser();
        // Author thấy TẤT CẢ truyện của mình, kể cả soft-deleted
        return storyRepository.findByAuthorId(currentUser.getId()).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public ChapterStatsResponse getChapterStats(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found with id: " + storyId));

        int publishedCount = (int) chapterRepository.countByStoryIdAndStatus(storyId, "PUBLISHED");
        int totalCount     = (int) chapterRepository.countByStoryId(storyId);

        return ChapterStatsResponse.builder()
                .storyId(storyId)
                .storyTitle(story.getTitle())
                .publishedCount(publishedCount)
                .totalCount(totalCount)
                .build();
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────
    @Override
    public StoryResponse updateStory(Long id, StoryRequest request) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        if (!story.getAuthor().getId().equals(currentUser.getId()))
            throw new UnauthorizedException("You don't have permission to update this story");

        if ("PENDING".equals(story.getStatus()))
            throw new BadRequestException("Cannot update story in PENDING status");

        story.setTitle(request.getTitle());
        story.setSummary(request.getSummary());
        story.setCoverUrl(request.getCoverUrl());

        // Cập nhật thể loại nếu được truyền vào
        if (request.getCategoryIds() != null) {
            story.setCategories(resolveCategories(request.getCategoryIds()));
        }

        return mapToResponse(storyRepository.save(story));
    }

    // ─────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────
    @Override
    public void deleteStory(Long id) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        if (!story.getAuthor().getId().equals(currentUser.getId()))
            throw new UnauthorizedException("You don't have permission to delete this story");

        // Soft delete — ẩn khỏi trang chủ nhưng không xóa dữ liệu
        story.setIsDeleted(true);
        storyRepository.save(story);
    }

    // ─────────────────────────────────────────────
    // SUBMIT FOR REVIEW
    // ─────────────────────────────────────────────
    @Override
    public StoryResponse submitForReview(Long id) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        if (!story.getAuthor().getId().equals(currentUser.getId()))
            throw new UnauthorizedException("You don't have permission to submit this story");

        if (Boolean.TRUE.equals(story.getIsDeleted()))
            throw new BadRequestException("Cannot submit a deleted story");

        if (!"DRAFT".equals(story.getStatus()) && !"REJECTED".equals(story.getStatus()))
            throw new BadRequestException("Can only submit stories in DRAFT or REJECTED status");

        story.setStatus("PENDING");
        story.setRejectReason(null);
        return mapToResponse(storyRepository.save(story));
    }

    // ─────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────
    @Override
    public List<StoryResponse> searchStories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return getAllPublishedStories();
        return storyRepository.searchPublished(keyword).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StoryResponse> getStoriesByCategory(Long categoryId) {
        return storyRepository.findByCategoryId(categoryId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StoryResponse> getTopViewedStories() {
        return storyRepository.findTopByViewCount().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StoryResponse> getTopRatedStories() {
        return storyRepository.findTopRated().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StoryResponse> getCompletedStories() {
        return storyRepository.findCompleted().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private StoryResponse mapToResponse(Story story) {
        Set<CategoryResponse> categories = story.getCategories() == null ? Set.of() :
                story.getCategories().stream()
                        .map(c -> CategoryResponse.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .build())
                        .collect(Collectors.toSet());

        int publishedCount = (int) chapterRepository.countByStoryIdAndStatus(story.getId(), "PUBLISHED");
        int totalCount     = (int) chapterRepository.countByStoryId(story.getId());
        long followCount   = userRepository.countFollowersByStoryId(story.getId());

        // isFollowing: chỉ kiểm tra nếu user đã đăng nhập
        Boolean isFollowing = null;
        try {
            User currentUser = userService.getCurrentUser();
            isFollowing = currentUser.getFollowedStories().stream()
                    .anyMatch(s -> s.getId().equals(story.getId()));
        } catch (Exception ignored) {
            // chưa đăng nhập → null
        }

        return StoryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .summary(story.getSummary())
                .coverUrl(story.getCoverUrl())
                .status(story.getStatus())
                .authorId(story.getAuthor().getId())
                .authorName(story.getAuthor().getFullName())
                .categories(categories)
                .viewCount(story.getViewCount() != null ? story.getViewCount() : 0L)
                .avgRating(story.getAvgRating() != null ? story.getAvgRating() : 0.0)
                .ratingCount(story.getRatingCount() != null ? story.getRatingCount() : 0)
                .isCompleted(Boolean.TRUE.equals(story.getIsCompleted()))
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .publishedChapterCount(publishedCount)
                .totalChapterCount(totalCount)
                .isDeleted(Boolean.TRUE.equals(story.getIsDeleted()))
                .followCount(followCount)
                .isFollowing(isFollowing)
                .build();
    }
}
