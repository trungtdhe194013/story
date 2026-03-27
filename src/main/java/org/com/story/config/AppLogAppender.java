package org.com.story.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.com.story.dto.response.SystemLogResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Logback appender — captures real application log events into AppLogStore.
 *
 * Rules:
 *  - Always capture ERROR and WARN from any logger.
 *  - Capture INFO and DEBUG only from our app package (org.com.story).
 *  - Ignore noisy framework logs (Hibernate SQL, Spring internals).
 *
 * Registered in logback-spring.xml.
 */
public class AppLogAppender extends AppenderBase<ILoggingEvent> {

    private static final String APP_PACKAGE = "org.com.story";
    private static final AtomicLong traceCounter = new AtomicLong(1);

    @Override
    protected void append(ILoggingEvent event) {
        // Spring hasn't started yet — store not ready
        AppLogStore store = AppLogStore.getInstance();
        if (store == null) return;

        if (!shouldCapture(event)) return;

        String component = extractComponent(event.getLoggerName());
        String message   = event.getFormattedMessage();

        // Skip empty or very short messages
        if (message == null || message.isBlank()) return;

        SystemLogResponse log = SystemLogResponse.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.getTimeStamp()),
                        ZoneId.of("Asia/Ho_Chi_Minh")))
                .severity(event.getLevel().toString())
                .component(component)
                .message(truncate(message, 500))
                .traceId("trace-" + String.format("%03d", traceCounter.getAndIncrement() % 1000))
                .build();

        store.addLog(log);
    }

    private boolean shouldCapture(ILoggingEvent event) {
        String level  = event.getLevel().toString();
        String logger = event.getLoggerName();

        // Always capture ERROR and WARN regardless of logger
        if ("ERROR".equals(level) || "WARN".equals(level)) return true;

        // Capture INFO and DEBUG only from our application code
        if (logger != null && logger.startsWith(APP_PACKAGE)) {
            return "INFO".equals(level) || "DEBUG".equals(level);
        }

        return false;
    }

    /**
     * Extract a human-readable component name from the logger name.
     * e.g. "org.com.story.service.impl.ChapterServiceImpl" -> "ChapterServiceImpl"
     */
    private String extractComponent(String loggerName) {
        if (loggerName == null || loggerName.isBlank()) return "Unknown";
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}

