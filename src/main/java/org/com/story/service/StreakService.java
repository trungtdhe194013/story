package org.com.story.service;

import org.com.story.dto.response.StreakResponse;

public interface StreakService {
    StreakResponse checkIn();
    StreakResponse getMyStreak();
}

