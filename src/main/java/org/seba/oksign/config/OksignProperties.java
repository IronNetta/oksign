package org.seba.oksign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oksign")
public class OksignProperties {
    private String apiKey;
    private String baseUrl;
    private String webhookSecret;
}
