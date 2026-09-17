package com.example.flowershop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sepay")
public class SePayProperties {

    private String merchantId;
    private String secretKey;
    private String checkoutUrl;
    private String feBaseUrl;
    private String successUrl;
    private String errorUrl;
    private String cancelUrl;
    private String ipnSecret;
    private String bankId;
    private String accountNumber;
    private String accountName;
    private String vaPrefix;
    private String apiKey;
}

