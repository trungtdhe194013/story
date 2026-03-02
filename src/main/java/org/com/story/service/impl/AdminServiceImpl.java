package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.DashboardStatsResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.dto.response.UserResponse;
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
        return storyRepository.findByStatus("PENDING")
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
            chapter.setStatus("PUBLISHED");
            if (chapter.getPublishAt() == null) {
                chapter.setPublishAt(java.time.LocalDateTime.now());
            }
        } else {
            // REJECT → trả về DRAFT để author sửa lại
            chapter.setStatus("DRAFT");
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

    private ChapterResponse mapChapterToResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .title(chapter.getTitle())
                .chapterOrder(chapter.getChapterOrder())
                .coinPrice(chapter.getCoinPrice())
                .status(chapter.getStatus())
                .publishAt(chapter.getPublishAt())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .isPurchased(false)
                .build();
    }

    private StoryResponse mapStoryToResponse(Story story) {
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
                .build();
    }
}
