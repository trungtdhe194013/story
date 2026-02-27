package org.com.story.service;

import org.com.story.dto.request.EditorChapterEditRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.ChapterVersionResponse;

import java.util.List;

public interface EditorService {
    List<ChapterResponse> getPendingChaptersForEdit();
    ChapterResponse assignChapterToEditor(Long chapterId);
    ChapterResponse submitChapterEdit(Long chapterId, EditorChapterEditRequest request);
    List<ChapterVersionResponse> getChapterVersions(Long chapterId);
}

