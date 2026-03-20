package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CreateEditRequestDto;
import org.com.story.dto.request.RejectEditDto;
import org.com.story.dto.request.SubmitEditDto;
import org.com.story.dto.response.EditRequestResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.*;
import org.com.story.service.EditRequestService;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EditRequestServiceImpl implements EditRequestService {

    private final EditRequestRepository editRequestRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterVersionRepository chapterVersionRepository;
    private final UserService userService;
    private final WalletService walletService;

    private static final List<String> ACTIVE_STATUSES =
            List.of("OPEN", "IN_PROGRESS", "SUBMITTED");

    // ==================== AUTHOR SIDE ====================

    @Override
    public EditRequestResponse createRequest(CreateEditRequestDto dto) {
        User author = userService.getCurrentUser();

        Chapter chapter = chapterRepository.findById(dto.getChapterId())
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Author phải sở hữu chapter
        if (!chapter.getStory().getAuthor().getId().equals(author.getId())) {
            throw new UnauthorizedException("Bạn không phải tác giả của chapter này");
        }

        // Chapter phải ở DRAFT để request edit
        if (!"DRAFT".equals(chapter.getStatus()) && !"EDITED".equals(chapter.getStatus())) {
            throw new BadRequestException("Chỉ chapter DRAFT hoặc EDITED mới có thể tạo edit request");
        }

        // Chapter chưa có request đang active
        editRequestRepository.findByChapterIdAndStatusIn(chapter.getId(), ACTIVE_STATUSES)
                .ifPresent(r -> {
                    throw new BadRequestException("Chapter này đã có edit request đang hoạt động (ID: " + r.getId() + ")");
                });

        // Lock coin ngay lập tức
        walletService.lockCoins(author.getId(), dto.getCoinReward(), null);

        EditRequest request = EditRequest.builder()
                .chapter(chapter)
                .author(author)
                .coinReward(dto.getCoinReward())
                .description(dto.getDescription())
                .status("OPEN")
                .attemptCount(0)
                .build();

        EditRequest saved = editRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditRequestResponse> getMyRequestsAsAuthor() {
        User author = userService.getCurrentUser();
        return editRequestRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public EditRequestResponse approveEdit(Long requestId) {
        User author = userService.getCurrentUser();
        EditRequest req = findAndValidateOwner(requestId, author);

        if (!"SUBMITTED".equals(req.getStatus())) {
            throw new BadRequestException("Chỉ có thể approve khi Editor đã nộp bản (SUBMITTED). Hiện tại: " + req.getStatus());
        }

        // Lưu nội dung cũ vào ChapterVersion trước khi ghi đè
        Chapter chapter = req.getChapter();
        long versionCount = chapterVersionRepository.countByChapterId(chapter.getId());
        ChapterVersion snapshot = new ChapterVersion();
        snapshot.setChapter(chapter);
        snapshot.setContent(chapter.getContent());
        snapshot.setVersion((int) (versionCount + 1));
        chapterVersionRepository.save(snapshot);

        // Cập nhật nội dung chapter từ bản edit được approve
        chapter.setContent(req.getEditedContent());
        chapter.setStatus("EDITED");
        chapterRepository.save(chapter);

        // Chuyển coin escrow → ví Editor
        walletService.releaseCoinsToEditor(
                author.getId(),
                req.getEditor().getId(),
                req.getCoinReward(),
                req.getId());

        req.setStatus("APPROVED");
        return mapToResponse(editRequestRepository.save(req));
    }

    @Override
    public EditRequestResponse rejectEdit(Long requestId, RejectEditDto dto) {
        User author = userService.getCurrentUser();
        EditRequest req = findAndValidateOwner(requestId, author);

        if (!"SUBMITTED".equals(req.getStatus())) {
            throw new BadRequestException("Chỉ có thể reject khi Editor đã nộp bản (SUBMITTED). Hiện tại: " + req.getStatus());
        }

        // Editor có thể viết lại — quay về IN_PROGRESS
        req.setStatus("IN_PROGRESS");
        req.setAuthorNote(dto.getAuthorNote());
        req.setAttemptCount(req.getAttemptCount() + 1);

        return mapToResponse(editRequestRepository.save(req));
    }

    @Override
    public EditRequestResponse cancelRequest(Long requestId) {
        User author = userService.getCurrentUser();
        EditRequest req = findAndValidateOwner(requestId, author);

        if (!"OPEN".equals(req.getStatus())) {
            throw new BadRequestException("Chỉ có thể huỷ khi chưa có Editor nhận (OPEN). Hiện tại: " + req.getStatus());
        }

        // Hoàn trả coin đã lock
        walletService.refundCoinsToAuthor(author.getId(), req.getCoinReward(), req.getId());
        req.setStatus("CANCELLED");

        return mapToResponse(editRequestRepository.save(req));
    }

    // ==================== EDITOR SIDE ====================

    @Override
    @Transactional(readOnly = true)
    public List<EditRequestResponse> getOpenRequests() {
        return editRequestRepository.findByStatus("OPEN")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public EditRequestResponse assignRequest(Long requestId) {
        User editor = userService.getCurrentUser();
        EditRequest req = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Edit request not found"));

        if (!"OPEN".equals(req.getStatus())) {
            throw new BadRequestException("Request không còn OPEN. Hiện tại: " + req.getStatus());
        }

        req.setEditor(editor);
        req.setStatus("IN_PROGRESS");

        return mapToResponse(editRequestRepository.save(req));
    }

    @Override
    public EditRequestResponse submitEdit(Long requestId, SubmitEditDto dto) {
        User editor = userService.getCurrentUser();
        EditRequest req = findAndValidateEditor(requestId, editor);

        if (!"IN_PROGRESS".equals(req.getStatus())) {
            throw new BadRequestException("Chỉ có thể submit khi đang IN_PROGRESS. Hiện tại: " + req.getStatus());
        }

        req.setEditedContent(dto.getEditedContent());
        req.setEditorNote(dto.getEditorNote());
        req.setStatus("SUBMITTED");

        return mapToResponse(editRequestRepository.save(req));
    }

    @Override
    public EditRequestResponse withdrawFromRequest(Long requestId) {
        User editor = userService.getCurrentUser();
        EditRequest req = findAndValidateEditor(requestId, editor);

        if (!"IN_PROGRESS".equals(req.getStatus())) {
            throw new BadRequestException("Chỉ có thể rút lui khi đang IN_PROGRESS. Hiện tại: " + req.getStatus());
        }

        // Editor chỉ được rút khi chưa submit lần nào (attemptCount == 0)
        if (req.getAttemptCount() > 0) {
            throw new BadRequestException("Bạn đã bị từ chối " + req.getAttemptCount() + " lần. Không thể rút lui sau khi đã submit — hãy tiếp tục hoàn thiện.");
        }

        req.setEditor(null);
        req.setStatus("OPEN");

        return mapToResponse(editRequestRepository.save(req));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditRequestResponse> getMyRequestsAsEditor() {
        User editor = userService.getCurrentUser();
        return editRequestRepository.findByEditorIdOrderByCreatedAtDesc(editor.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ==================== HELPERS ====================

    private EditRequest findAndValidateOwner(Long requestId, User author) {
        EditRequest req = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Edit request not found"));
        if (!req.getAuthor().getId().equals(author.getId())) {
            throw new UnauthorizedException("Bạn không phải tác giả của request này");
        }
        return req;
    }

    private EditRequest findAndValidateEditor(Long requestId, User editor) {
        EditRequest req = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Edit request not found"));
        if (req.getEditor() == null || !req.getEditor().getId().equals(editor.getId())) {
            throw new UnauthorizedException("Bạn không phải editor được gán cho request này");
        }
        return req;
    }

    private EditRequestResponse mapToResponse(EditRequest req) {
        // Chỉ trả editedContent khi SUBMITTED hoặc APPROVED
        String editedContent = null;
        if ("SUBMITTED".equals(req.getStatus()) || "APPROVED".equals(req.getStatus())) {
            editedContent = req.getEditedContent();
        }

        return EditRequestResponse.builder()
                .id(req.getId())
                .chapterId(req.getChapter().getId())
                .chapterTitle(req.getChapter().getTitle())
                .storyTitle(req.getChapter().getStory().getTitle())
                .authorId(req.getAuthor().getId())
                .authorName(req.getAuthor().getFullName())
                .editorId(req.getEditor() != null ? req.getEditor().getId() : null)
                .editorName(req.getEditor() != null ? req.getEditor().getFullName() : null)
                .coinReward(req.getCoinReward())
                .description(req.getDescription())
                .editedContent(editedContent)
                .editorNote(req.getEditorNote())
                .authorNote(req.getAuthorNote())
                .status(req.getStatus())
                .attemptCount(req.getAttemptCount())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }
}

