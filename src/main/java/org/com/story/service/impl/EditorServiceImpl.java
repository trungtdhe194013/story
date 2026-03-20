package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.ChapterVersionResponse;
import org.com.story.entity.ChapterVersion;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.ChapterRepository;
import org.com.story.repository.ChapterVersionRepository;
import org.com.story.service.EditorService;
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

