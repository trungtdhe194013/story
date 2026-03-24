package org.com.story.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Gắn header "ngrok-skip-browser-warning: true" vào tất cả response tĩnh (/uploads/**)
 * để ngrok free tier không hiển thị trang cảnh báo khi frontend load ảnh.
 */
@Component
@Order(1)
public class NgrokHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();

        // Chỉ gắn header cho /uploads/** (ảnh tĩnh)
        if (path.startsWith("/uploads/")) {
            httpResp.setHeader("ngrok-skip-browser-warning", "true");
            httpResp.setHeader("Access-Control-Allow-Origin", "*");
        }

        chain.doFilter(request, response);
    }
}

