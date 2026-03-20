package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CommentRequest;
import org.com.story.dto.response.CommentResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.CommentRepository;
import org.com.story.repository.StoryRepository;
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
    private final StoryRepository storyRepository;
    private final UserService userService;

    @Override
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = userService.getCurrentUser();

        Comment comment = new Comment();
        comment.setUser(currentUser);
        comment.setContent(request.getContent());

        if (request.getChapterId() != null) {
            Chapter chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new NotFoundException("Chapter not found"));
            comment.setChapter(chapter);
        } else if (request.getStoryId() != null) {
            Story story = storyRepository.findById(request.getStoryId())
                    .orElseThrow(() -> new NotFoundException("Story not found"));
            comment.setStory(story);
        } else {
            throw new BadRequestException("Either chapterId or storyId is required");
        }

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));
            comment.setParent(parent);
        }

        return mapToResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByChapter(Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        List<Comment> allComments = commentRepository.findByChapterId(chapterId);
        return buildCommentTree(allComments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByStory(Long storyId) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        List<Comment> allComments = commentRepository.findByStoryId(storyId);
        return buildCommentTree(allComments);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        List<Comment> allInContext;
        if (comment.getChapter() != null) {
            allInContext = commentRepository.findByChapterId(comment.getChapter().getId());
        } else if (comment.getStory() != null) {
            allInContext = commentRepository.findByStoryId(comment.getStory().getId());
        } else {
            return mapToResponse(comment);
        }

        Map<Long, List<Comment>> childrenMap = allInContext.stream()
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
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You don't have permission to delete this comment");
        }
        commentRepository.delete(comment);
    }

    private List<CommentResponse> buildCommentTree(List<Comment> allComments) {
        // Filter hidden
        List<Comment> visibleComments = allComments.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getHidden()))
                .collect(Collectors.toList());

        Map<Long, List<Comment>> childrenMap = visibleComments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return visibleComments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> mapToResponseRecursive(c, childrenMap))
                .collect(Collectors.toList());
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .chapterId(comment.getChapter() != null ? comment.getChapter().getId() : null)
                .chapterTitle(comment.getChapter() != null ? comment.getChapter().getTitle() : null)
                .chapterOrder(comment.getChapter() != null ? comment.getChapter().getChapterOrder() : null)
                .storyId(comment.getStory() != null ? comment.getStory().getId() :
                        (comment.getChapter() != null ? comment.getChapter().getStory().getId() : null))
                .storyTitle(comment.getStory() != null ? comment.getStory().getTitle() :
                        (comment.getChapter() != null ? comment.getChapter().getStory().getTitle() : null))
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFullName())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .hidden(comment.getHidden())
                .likeCount(comment.getLikeCount())
                .replies(new ArrayList<>())
                .createdAt(comment.getCreatedAt())
                .build();
    }

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
