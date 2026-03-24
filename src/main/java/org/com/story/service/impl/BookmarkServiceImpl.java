package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.BookmarkResponse;
import org.com.story.entity.*;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.BookmarkRepository;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.service.BookmarkService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserService userService;

    @Override
    public BookmarkResponse saveBookmark(Long storyId, Long chapterId) {
        User currentUser = userService.getCurrentUser();

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Upsert: nếu đã có bookmark cho story này thì cập nhật chapter
        Bookmark bookmark = bookmarkRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseGet(() -> Bookmark.builder()
                        .user(currentUser)
                        .story(story)
                        .build());

        bookmark.setChapter(chapter);
        bookmark.setUpdatedAt(LocalDateTime.now());
        return toResponse(bookmarkRepository.save(bookmark));
    }

    @Override
    public void removeBookmark(Long storyId) {
        User currentUser = userService.getCurrentUser();
        bookmarkRepository.deleteByUserIdAndStoryId(currentUser.getId(), storyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkResponse> getMyBookmarks() {
        User currentUser = userService.getCurrentUser();
        return bookmarkRepository.findByUserIdOrderByUpdatedAtDesc(currentUser.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookmarkResponse getBookmark(Long storyId) {
        User currentUser = userService.getCurrentUser();
        return bookmarkRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .map(this::toResponse)
                .orElse(null);
    }

    private BookmarkResponse toResponse(Bookmark b) {
        return BookmarkResponse.builder()
                .id(b.getId())
                .storyId(b.getStory().getId())
                .storyTitle(b.getStory().getTitle())
                .storyCoverUrl(b.getStory().getCoverUrl())
                .chapterId(b.getChapter().getId())
                .chapterTitle(b.getChapter().getTitle())
                .chapterOrder(b.getChapter().getChapterOrder())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}

