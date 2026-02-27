package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.EditorChapterEditRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.ChapterVersionResponse;
import org.com.story.entity.Chapter;
import org.com.story.entity.ChapterVersion;
import org.com.story.entity.User;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.ChapterVersionRepository;
import org.com.story.service.EditorService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EditorServiceImpl implements EditorService {

    private final ChapterRepository chapterRepository;
    private final ChapterVersionRepository chapterVersionRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponse> getPendingChaptersForEdit() {
        // Lấy tất cả chapter ở trạng thái DRAFT mà chưa có editor
        List<Chapter> chapters = chapterRepository.findAll().stream()
                .filter(c -> "DRAFT".equals(c.getStatus()) && c.getEditor() == null)
                .collect(Collectors.toList());

        return chapters.stream()
                .map(this::mapToChapterResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChapterResponse assignChapterToEditor(Long chapterId) {
        User currentUser = userService.getCurrentUser();

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        if (chapter.getEditor() != null) {
            throw new BadRequestException("Chapter is already assigned to an editor");
        }

        chapter.setEditor(currentUser);
        Chapter updated = chapterRepository.save(chapter);
        return mapToChapterResponse(updated);
    }

    @Override
    public ChapterResponse submitChapterEdit(Long chapterId, EditorChapterEditRequest request) {
        User currentUser = userService.getCurrentUser();

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        // Kiểm tra editor đã được gán
        if (chapter.getEditor() == null || !chapter.getEditor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not assigned to edit this chapter");
        }

        // Lưu version cũ trước khi sửa
        long versionCount = chapterVersionRepository.countByChapterId(chapterId);
        ChapterVersion version = new ChapterVersion();
        version.setChapter(chapter);
        version.setContent(chapter.getContent());
        version.setVersion((int) (versionCount + 1));
        chapterVersionRepository.save(version);

        // Cập nhật nội dung chapter
        chapter.setContent(request.getContent());
        chapter.setStatus("EDITED"); // Đánh dấu đã được editor chỉnh sửa

        Chapter updated = chapterRepository.save(chapter);
        return mapToChapterResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterVersionResponse> getChapterVersions(Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        return chapterVersionRepository.findByChapterIdOrderByVersionDesc(chapterId)
                .stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    private ChapterResponse mapToChapterResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .chapterOrder(chapter.getChapterOrder())
                .coinPrice(chapter.getCoinPrice())
                .status(chapter.getStatus())
                .publishAt(chapter.getPublishAt())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .isPurchased(false)
                .build();
    }

    private ChapterVersionResponse mapToVersionResponse(ChapterVersion version) {
        return ChapterVersionResponse.builder()
                .id(version.getId())
                .chapterId(version.getChapter().getId())
                .content(version.getContent())
                .version(version.getVersion())
                .createdAt(version.getCreatedAt())
                .build();
    }
}

