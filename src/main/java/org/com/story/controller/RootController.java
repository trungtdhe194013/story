package org.com.story.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Root Controller - Welcome page for API
 * Useful when accessing via ngrok or direct URL
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "online");
        response.put("message", "Welcome to Story API");
        response.put("swagger", "/swagger-ui/index.html");
        response.put("health", "/api/health");
        response.put("version", "1.0.0");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("Auth", "/api/auth/**");
        endpoints.put("Stories", "/api/stories/**");
        endpoints.put("Chapters", "/api/chapters/**");
        endpoints.put("Users", "/api/users/**");
        endpoints.put("Admin", "/api/admin/**");
        endpoints.put("Reviewer", "/api/reviewer/**");

        response.put("endpoints", endpoints);

        return response;
    }
}
