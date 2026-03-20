package org.com.story.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payos")
public class PayOSProperties {

    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String returnUrl;
    private String cancelUrl;
    /** URL backend nhận IPN từ PayOS — phải là URL public (ngrok hoặc production) */
    private String webhookUrl;
    private Long minWithdraw = 50_000L;
}


