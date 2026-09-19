package com.bob.api.integration.facebook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bob.facebook")
public record FacebookProperties(
        String appId,
        String appSecret,
        String verifyToken,
        String apiVersion
) {
    public FacebookProperties {
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = "v18.0";
        }
        if (appId == null) {
            appId = "";
        }
        if (appSecret == null) {
            appSecret = "";
        }
        if (verifyToken == null) {
            verifyToken = "";
        }
    }

    public String graphBaseUrl() {
        return "https://graph.facebook.com/" + apiVersion;
    }
}
