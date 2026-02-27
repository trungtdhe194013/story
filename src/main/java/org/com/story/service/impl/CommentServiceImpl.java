package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CommentRequest;
import org.com.story.dto.response.CommentResponse;
import org.com.story.entity.Chapter;
import org.com.story.entity.Comment;
import org.com.story.entity.User;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.CommentRepository;
import org.com.story.service.CommentService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ChapterRepository chapterRepository;
    private final UserService userService;

    @Override
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = userService.getCurrentUser();

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        Comment comment = new Comment();
        comment.setUser(currentUser);
        comment.setChapter(chapter);
        comment.setContent(request.getContent());

        // Nếu là reply
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));

            if (!parent.getChapter().getId().equals(chapter.getId())) {
                throw new BadRequestException("Parent comment does not belong to this chapter");
            }
            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByChapter(Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Lấy comment gốc (không có parent)
        List<Comment> rootComments = commentRepository.findByChapterIdAndParentIsNull(chapterId);

        return rootComments.stream()
                .map(this::mapToResponseWithReplies)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long id) {
        User currentUser = userService.getCurrentUser();

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        // Chỉ cho phép xóa comment của chính mình hoặc admin
        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You don't have permission to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .chapterId(comment.getChapter().getId())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFullName())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(new ArrayList<>())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private CommentResponse mapToResponseWithReplies(Comment comment) {
        // Lấy tất cả replies của comment
        List<Comment> replies = commentRepository.findByChapterId(comment.getChapter().getId())
                .stream()
                .filter(c -> c.getParent() != null && c.getParent().getId().equals(comment.getId()))
                .collect(Collectors.toList());

        CommentResponse response = mapToResponse(comment);
        response.setReplies(replies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));

        return response;
    }
}

