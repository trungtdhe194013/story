package org.com.story.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter cho ảnh tĩnh /uploads/**:
 * - Gắn "ngrok-skip-browser-warning: true" vào RESPONSE (để script/fetch từ browser biết OK)
 * - Gắn "Access-Control-Allow-Origin: *" để frontend load ảnh cross-origin
 *
 * LƯU Ý: Ngrok free tier yêu cầu query param "?ngrok-skip-browser-warning=true" trong URL
 * khi img tag load ảnh (vì img tag không gửi được custom header).
 * URL sinh ra từ UploadController đã tự động append query param này khi base-url là ngrok.
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

        if (path.startsWith("/uploads/")) {
            httpResp.setHeader("ngrok-skip-browser-warning", "true");
            httpResp.setHeader("Access-Control-Allow-Origin", "*");
            httpResp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            httpResp.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
        }

        chain.doFilter(request, response);
    }
}

