package com.chatwoot.api.integration.facebook.service;

import com.chatwoot.api.integration.facebook.config.FacebookProperties;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class FacebookGraphClientImpl implements FacebookGraphClient {

    private static final Logger log = LoggerFactory.getLogger(FacebookGraphClientImpl.class);
    private static final String SUBSCRIBED_FIELDS =
            "messages,message_deliveries,message_echoes,message_reads,standby,messaging_handovers,messaging_postbacks";

    private final FacebookProperties properties;
    private final RestClient restClient;

    public FacebookGraphClientImpl(FacebookProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String exchangeLongLivedToken(String shortLivedToken) {
        JsonNode body = restClient.get()
                .uri(properties.graphBaseUrl() + "/oauth/access_token"
                        + "?grant_type=fb_exchange_token"
                        + "&client_id={clientId}"
                        + "&client_secret={clientSecret}"
                        + "&fb_exchange_token={token}",
                        properties.appId(), properties.appSecret(), shortLivedToken)
                .retrieve()
                .body(JsonNode.class);
        if (body == null || body.path("access_token").asText("").isBlank()) {
            throw new IllegalStateException("Facebook token exchange returned no access_token");
        }
        return body.path("access_token").asText();
    }

    @Override
    public List<FacebookAccountPage> listPages(String userAccessToken) {
        List<FacebookAccountPage> pages = new ArrayList<>();
        String url = properties.graphBaseUrl() + "/me/accounts?access_token={token}";
        JsonNode body = restClient.get()
                .uri(url, userAccessToken)
                .retrieve()
                .body(JsonNode.class);
        collectPages(body, pages);
        while (body != null && body.path("paging").path("next").isTextual()) {
            String next = body.path("paging").path("next").asText();
            body = restClient.get().uri(URI.create(next)).retrieve().body(JsonNode.class);
            collectPages(body, pages);
        }
        return pages;
    }

    @Override
    public FacebookPageDetails fetchPageDetails(String pageAccessToken) {
        JsonNode body = restClient.get()
                .uri(properties.graphBaseUrl() + "/me?fields=name,instagram_business_account&access_token={token}",
                        pageAccessToken)
                .retrieve()
                .body(JsonNode.class);
        if (body == null) {
            return new FacebookPageDetails(null, null);
        }
        String instagramId = body.path("instagram_business_account").path("id").asText(null);
        return new FacebookPageDetails(textOrNull(body.path("name")), instagramId);
    }

    @Override
    public void subscribePage(String pageId, String pageAccessToken) {
        restClient.post()
                .uri(properties.graphBaseUrl() + "/{pageId}/subscribed_apps"
                        + "?access_token={token}&subscribed_fields={fields}",
                        pageId, pageAccessToken, SUBSCRIBED_FIELDS)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public FacebookSendResult sendText(String pageAccessToken, String recipientPsid, String text) {
        String payload = """
                {"recipient":{"id":"%s"},"message":{"text":%s},"messaging_type":"RESPONSE"}
                """.formatted(escape(recipientPsid), jsonString(text));
        try {
            JsonNode body = restClient.post()
                    .uri(properties.graphBaseUrl() + "/me/messages?access_token={token}", pageAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return new FacebookSendResult(null, null, "Empty response from Facebook");
            }
            if (body.has("error")) {
                JsonNode error = body.get("error");
                return new FacebookSendResult(
                        null,
                        error.path("code").asText(null),
                        error.path("message").asText("Facebook error")
                );
            }
            return new FacebookSendResult(textOrNull(body.path("message_id")), null, null);
        } catch (RestClientResponseException ex) {
            log.warn("Facebook send HTTP error: {}", ex.getResponseBodyAsString());
            return parseErrorBody(ex.getResponseBodyAsString(), ex.getMessage());
        } catch (RestClientException ex) {
            log.warn("Facebook send failed: {}", ex.getMessage());
            return new FacebookSendResult(null, null, ex.getMessage());
        }
    }

    @Override
    public FacebookUserProfile fetchUserProfile(String pageAccessToken, String psid) {
        try {
            JsonNode body = restClient.get()
                    .uri(properties.graphBaseUrl() + "/{psid}?fields=first_name,last_name&access_token={token}",
                            psid, pageAccessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return new FacebookUserProfile(null, null);
            }
            return new FacebookUserProfile(textOrNull(body.path("first_name")), textOrNull(body.path("last_name")));
        } catch (RestClientException ex) {
            log.warn("Facebook profile fetch failed for {}: {}", psid, ex.getMessage());
            return new FacebookUserProfile(null, null);
        }
    }

    private void collectPages(JsonNode body, List<FacebookAccountPage> pages) {
        if (body == null || !body.path("data").isArray()) {
            return;
        }
        for (JsonNode page : body.path("data")) {
            pages.add(new FacebookAccountPage(
                    textOrNull(page.path("id")),
                    textOrNull(page.path("name")),
                    textOrNull(page.path("access_token"))
            ));
        }
    }

    private FacebookSendResult parseErrorBody(String raw, String fallback) {
        if (raw != null && (raw.contains("The session has been invalidated")
                || raw.contains("Error validating access token"))) {
            return new FacebookSendResult(null, null, raw);
        }
        return new FacebookSendResult(null, null, fallback);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value).replace("\n", "\\n") + "\"";
    }
}
