package org.com.story.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.story.exception.ErrorResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Tự động bọc tất cả response từ controller thành ApiResponse chuẩn.
 * Frontend sẽ luôn nhận được format:
 * { "success": true, "status": 200, "data": { ... } }
 * hoặc
 * { "success": false, "status": 400, "message": "..." }
 */
@RestControllerAdvice(basePackages = "org.com.story.controller")
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Bỏ qua các response đã là ApiResponse hoặc ErrorResponse rồi
        Class<?> returnClass = returnType.getParameterType();
        return !ApiResponse.class.isAssignableFrom(returnClass)
                && !ErrorResponse.class.isAssignableFrom(returnClass)
                && !String.class.isAssignableFrom(returnClass);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // Bỏ qua swagger / openapi / health
        String path = request.getURI().getPath();
        if (path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator")
                || path.equals("/")) {
            return body;
        }

        // Nếu body null (ví dụ void method) → trả về noContent
        if (body == null) {
            return ApiResponse.noContent();
        }

        // Nếu đã là ApiResponse hoặc ErrorResponse → giữ nguyên
        if (body instanceof ApiResponse || body instanceof ErrorResponse) {
            return body;
        }

        // Wrap vào ApiResponse.ok
        return ApiResponse.ok(body);
    }
}

