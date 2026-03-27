package org.com.story.config;

import org.com.story.dto.response.SystemLogResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory circular buffer for application logs.
 * AppLogAppender (Logback) writes here; AdminService reads from here.
 *
 * Uses a static INSTANCE so the Logback appender (which runs outside Spring context)
 * can access it before Spring finishes startup.
 */
@Component
public class AppLogStore {

    /** Max number of log entries kept in memory */
    private static final int MAX_SIZE = 1000;

    /** Static reference — set when Spring creates this bean */
    private static AppLogStore INSTANCE;

    /** Thread-safe deque — newest entries at the front */
    private final Deque<SystemLogResponse> buffer = new ConcurrentLinkedDeque<>();

    public AppLogStore() {
        INSTANCE = this;
    }

    /** Called by AppLogAppender (Logback thread, outside Spring context) */
    public static AppLogStore getInstance() {
        return INSTANCE;
    }

    public void addLog(SystemLogResponse entry) {
        buffer.addFirst(entry);
        // Trim tail when over capacity
        while (buffer.size() > MAX_SIZE) {
            buffer.pollLast();
        }
    }

    /** Return all entries newest-first */
    public List<SystemLogResponse> getAll() {
        return new ArrayList<>(buffer);
    }

    public int size() {
        return buffer.size();
    }
}

