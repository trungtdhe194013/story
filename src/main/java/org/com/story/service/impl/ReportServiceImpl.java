package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReportRequest;
import org.com.story.dto.request.ResolveReportRequest;
import org.com.story.dto.response.ReportResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.*;
import org.com.story.service.ReportService;
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
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    private static final Set<String> VALID_TARGET_TYPES = Set.of("STORY", "CHAPTER", "COMMENT");
    private static final Set<String> VALID_ACTIONS = Set.of(
            "WARN_ONLY", "HIDE_CONTENT", "DELETE_CONTENT",
            "BAN_USER", "HIDE_AND_BAN", "DELETE_AND_BAN"
    );

    @Override
    public ReportResponse createReport(ReportRequest request) {
        User currentUser = userService.getCurrentUser();

        if (!VALID_TARGET_TYPES.contains(request.getTargetType())) {
            throw new BadRequestException("Invalid target type. Must be STORY, CHAPTER, or COMMENT");
        }

        Report report = new Report();
        report.setReporter(currentUser);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus("PENDING");

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports() {
        User currentUser = userService.getCurrentUser();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getPendingReports() {
        return reportRepository.findByStatus("PENDING")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReportResponse resolveReport(Long id, ResolveReportRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        if ("RESOLVED".equals(report.getStatus())) {
            throw new BadRequestException("Report is already resolved");
        }

        String action = request.getAction().toUpperCase();
        if (!VALID_ACTIONS.contains(action)) {
            throw new BadRequestException("Invalid action. Allowed: " + VALID_ACTIONS);
        }

        // ---- 1. Thực hiện hành động lên nội dung bị báo cáo ----
        User targetOwner = null;

        boolean shouldHide   = action.equals("HIDE_CONTENT")   || action.equals("HIDE_AND_BAN");
        boolean shouldDelete = action.equals("DELETE_CONTENT")  || action.equals("DELETE_AND_BAN");
        boolean shouldBan    = action.equals("BAN_USER")        || action.equals("HIDE_AND_BAN") || action.equals("DELETE_AND_BAN");

        switch (report.getTargetType()) {
            case "COMMENT" -> {
                Comment comment = commentRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Comment #" + report.getTargetId() + " not found"));
                targetOwner = comment.getUser();
                if (shouldHide) {
                    comment.setHidden(true);
                    commentRepository.save(comment);
                } else if (shouldDelete) {
                    commentRepository.delete(comment);
                }
            }
            case "CHAPTER" -> {
                Chapter chapter = chapterRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Chapter #" + report.getTargetId() + " not found"));
                targetOwner = chapter.getStory().getAuthor();
                if (shouldHide) {
                    chapter.setStatus("HIDDEN");
                    chapterRepository.save(chapter);
                } else if (shouldDelete) {
                    chapterRepository.delete(chapter);
                }
            }
            case "STORY" -> {
                Story story = storyRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Story #" + report.getTargetId() + " not found"));
                targetOwner = story.getAuthor();
                if (shouldHide) {
                    story.setStatus("REJECTED");
                    storyRepository.save(story);
                } else if (shouldDelete) {
                    storyRepository.delete(story);
                }
            }
        }

        // ---- 2. Ban tài khoản tác giả nội dung (nếu cần) ----
        if (shouldBan && targetOwner != null) {
            if (request.getBanDays() == 0) {
                throw new BadRequestException("banDays must be > 0 or -1 (permanent) when action includes BAN");
            }
            if (request.getBanDays() == -1) {
                // Ban vĩnh viễn: đặt thời gian rất xa
                targetOwner.setBanUntil(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
            } else {
                targetOwner.setBanUntil(LocalDateTime.now().plusDays(request.getBanDays()));
            }
            userRepository.save(targetOwner);
        }

        // ---- 3. Đánh dấu report đã xử lý ----
        report.setStatus("RESOLVED");
        report.setResolvedAction(action);
        report.setAdminNote(request.getAdminNote());
        Report updated = reportRepository.save(report);
        return mapToResponse(updated);
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .status(report.getStatus())
                .resolvedAction(report.getResolvedAction())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

