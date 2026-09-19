package com.chatwoot.api.integration.facebook.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FacebookProperties.class)
public class FacebookConfig {
}
