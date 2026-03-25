package org.com.story.service;

public interface MailService {
    void sendTextMail(String to, String subject, String content);
    void sendHtmlMail(String to, String subject, String html);
}
