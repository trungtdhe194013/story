package org.com.story.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    // Get chapter detail (public/protected based on access)
    @GetMapping("/{id}")
    public ChapterResponse getChapter(@PathVariable Long id) {

        return chapterService.getChapter(id);
    }

    // Get chapters by story
    @GetMapping("/story/{storyId}")
    public List<ChapterResponse> getChaptersByStory(@PathVariable Long storyId) {
        return chapterService.getChaptersByStory(storyId);
    }

    // Create new chapter (author only)
    @PostMapping("/story/{storyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterResponse createChapter(
            @PathVariable Long storyId,
            @Valid @RequestBody ChapterRequest request) {
        return chapterService.createChapter(storyId, request);
    }

    // Update chapter (author only)
    @PutMapping("/{id}")
    public ChapterResponse updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        return chapterService.updateChapter(id, request);
    }

    // Delete chapter (author only)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(@PathVariable Long id) {

        chapterService.deleteChapter(id);
    }

    // Publish chapter (author only)
    @PostMapping("/{id}/publish")
    public ChapterResponse publishChapter(@PathVariable Long id) {

        return chapterService.publishChapter(id);
    }

    // Purchase chapter (reader)
    @PostMapping("/{id}/purchase")
    public ChapterResponse purchaseChapter(@PathVariable Long id) {

        return chapterService.purchaseChapter(id);
    }
}


