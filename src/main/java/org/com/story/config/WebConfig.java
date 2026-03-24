package org.com.story.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * Serve ảnh upload từ thư mục local: uploads/avatars/ và uploads/covers/
     * Header ngrok-skip-browser-warning: true → bỏ qua trang warning của ngrok free tier
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get("uploads").toAbsolutePath().toString();
        String resourceLocation = "file:" + absoluteUploadPath.replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                // Cache 1 ngày ở browser để tránh tải lại nhiều lần
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .resourceChain(true)
                // Thêm transformer để gắn header ngrok-skip-browser-warning vào response
                .addTransformer((request, resource, transformerChain) -> {
                    // Trả về resource gốc — header được gắn qua Filter bên dưới
                    return transformerChain.transform(request, resource);
                });
    }
}



