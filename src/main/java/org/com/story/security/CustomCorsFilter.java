package org.com.story.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ⚠️ Filter này đã bị vô hiệu hóa (shouldNotFilter luôn trả về true).
 *
 * Lý do: CORS đã được cấu hình đầy đủ trong SecurityConfig.corsConfigurationSource()
 * và WebConfig.addCorsMappings(). Filter này hard-code "localhost:3000" gây xung đột
 * khi deploy qua ngrok hoặc domain khác.
 *
 * Nếu cần CORS đặc biệt, hãy sửa SecurityConfig.corsConfigurationSource() thay vì filter này.
 */
@Component
public class CustomCorsFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Vô hiệu hóa hoàn toàn — CORS đã xử lý ở SecurityConfig
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        filterChain.doFilter(request, response);
    }
}
