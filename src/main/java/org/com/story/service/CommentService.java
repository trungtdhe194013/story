package org.com.story.service;

import org.com.story.dto.request.CommentRequest;
import org.com.story.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {
    CommentResponse createComment(CommentRequest request);
    List<CommentResponse> getCommentsByChapter(Long chapterId);
    void deleteComment(Long id);
}

