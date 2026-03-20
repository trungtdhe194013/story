package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.ReadingHistoryResponse;
import org.com.story.entity.Chapter;
import org.com.story.entity.ReadingHistory;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.ReadingHistoryRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.repository.UserRepository;
import org.com.story.service.ReadingHistoryService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void recordReading(Long userId, Long storyId, Long chapterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        ReadingHistory history = readingHistoryRepository.findByUserIdAndStoryId(userId, storyId)
                .orElse(ReadingHistory.builder()
                        .user(user)
                        .story(story)
                        .build());

        history.setLastChapter(chapter);
        readingHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingHistoryResponse> getMyReadingHistory() {
        User currentUser = userService.getCurrentUser();
        return readingHistoryRepository.findByUserIdOrderByLastReadAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReadingHistoryResponse mapToResponse(ReadingHistory h) {
        return ReadingHistoryResponse.builder()
                .storyId(h.getStory().getId())
                .storyTitle(h.getStory().getTitle())
                .storyCoverUrl(h.getStory().getCoverUrl())
                .authorName(h.getStory().getAuthor().getFullName())
                .lastChapterId(h.getLastChapter() != null ? h.getLastChapter().getId() : null)
                .lastChapterTitle(h.getLastChapter() != null ? h.getLastChapter().getTitle() : null)
                .lastChapterOrder(h.getLastChapter() != null ? h.getLastChapter().getChapterOrder() : null)
                .lastReadAt(h.getLastReadAt())
                .build();
    }
}

