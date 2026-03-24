package org.com.story.service;

import org.com.story.dto.response.BookmarkResponse;

import java.util.List;

public interface BookmarkService {

    /** Đặt / cập nhật bookmark cho một story tại chapter chỉ định */
    BookmarkResponse saveBookmark(Long storyId, Long chapterId);

    /** Xoá bookmark của story */
    void removeBookmark(Long storyId);

    /** Danh sách tất cả bookmark của user hiện tại (Library cá nhân) */
    List<BookmarkResponse> getMyBookmarks();

    /** Bookmark hiện tại của user cho một story (null nếu chưa có) */
    BookmarkResponse getBookmark(Long storyId);
}

