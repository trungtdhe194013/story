package org.com.story.service;

import org.com.story.dto.response.ChapterVersionResponse;

import java.util.List;

public interface EditorService {
    /** Xem lịch sử các phiên bản nội dung của một chapter */
    List<ChapterVersionResponse> getChapterVersions(Long chapterId);
}

