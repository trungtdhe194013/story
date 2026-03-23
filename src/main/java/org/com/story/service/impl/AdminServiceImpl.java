package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.ChapterSummaryResponse;
import org.com.story.dto.response.DashboardStatsResponse;
import org.com.story.dto.response.ReviewHistoryResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.dto.response.UserResponse;
import org.com.story.dto.response.CategoryResponse;
import org.com.story.entity.AdminReview;
import org.com.story.entity.Chapter;
import org.com.story.entity.Role;
import org.com.story.entity.Story;
import org.com.story.entity.User;
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
import org.com.story.repository.WithdrawRequestRepository;
import org.com.story.service.AdminService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

        return mapStoryToResponse(updatedStory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponse> getPendingChaptersForReview() {
        return chapterRepository.findByStatus("PENDING_REVIEW")
                .stream()
                .map(this::mapChapterToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChapterResponse reviewChapter(Long chapterId, ReviewChapterRequest request) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (!"PENDING_REVIEW".equals(chapter.getStatus())) {
            throw new BadRequestException("Chapter is not in PENDING_REVIEW status. Current status: " + chapter.getStatus());
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
