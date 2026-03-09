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
import java.util.Map;
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
        comment.setContent(request.getContent());
        comment.setChapter(chapter);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));
            if (parent.getChapter() == null || !parent.getChapter().getId().equals(chapter.getId())) {
                throw new BadRequestException("Parent comment does not belong to this chapter");
            }
            comment.setParent(parent);
        }

        return mapToResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByChapter(Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Load toàn bộ comment của chapter chỉ với 1 query
        List<Comment> allComments = commentRepository.findByChapterId(chapterId);

        // Group theo parentId để tra cứu O(1)
        Map<Long, List<Comment>> childrenMap = allComments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // Chỉ lấy root comments (parent == null) rồi build cây đệ quy
        return allComments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> mapToResponseRecursive(c, childrenMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        // Build childrenMap chỉ cho chapter của comment đó
        List<Comment> allInChapter = commentRepository.findByChapterId(comment.getChapter().getId());
        Map<Long, List<Comment>> childrenMap = allInChapter.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return mapToResponseRecursive(comment, childrenMap);
    }

    @Override
    public void deleteComment(Long id) {
        User currentUser = userService.getCurrentUser();
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You don't have permission to delete this comment");
        }
        commentRepository.delete(comment);
    }

    private CommentResponse mapToResponse(Comment comment) {
        Chapter chapter = comment.getChapter();
        return CommentResponse.builder()
                .id(comment.getId())
                .chapterId(chapter.getId())
                .chapterTitle(chapter.getTitle())
                .chapterOrder(chapter.getChapterOrder())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFullName())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .hidden(comment.getHidden())
                .replies(new ArrayList<>())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * Build cây comment đệ quy vô hạn cấp.
     * childrenMap: parentId → danh sách comment con (đã load sẵn, không query thêm)
     */
    private CommentResponse mapToResponseRecursive(Comment comment, Map<Long, List<Comment>> childrenMap) {
        CommentResponse response = mapToResponse(comment);
        List<Comment> children = childrenMap.getOrDefault(comment.getId(), new ArrayList<>());
        response.setReplies(
                children.stream()
                        .map(child -> mapToResponseRecursive(child, childrenMap))
                        .collect(Collectors.toList())
        );
        return response;
    }
}

