package com.chatwoot.api.integration.facebook.service;

import com.chatwoot.api.integration.facebook.config.FacebookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public FacebookGraphClientImpl(FacebookProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String exchangeLongLivedToken(String shortLivedToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.appId());
        form.add("client_secret", properties.appSecret());
        form.add("grant_type", "fb_exchange_token");
        form.add("fb_exchange_token", shortLivedToken);
        try {
            String body = restClient.post()
                    .uri(graphUri("/oauth/access_token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            String token = parseAccessToken(body);
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Facebook token exchange returned no access_token");
            }
            return token;
        } catch (RestClientResponseException ex) {
            log.error("Error in long_lived_token: {}", ex.getResponseBodyAsString());
            throw ex;
        }
    }

    @Override
    public List<FacebookAccountPage> listPages(String userAccessToken) {
        List<FacebookAccountPage> pages = new ArrayList<>();
        URI first = graphUri("/me/accounts", "access_token", userAccessToken);
        JsonNode body = getJson(first);
        collectPages(body, pages);
        while (body != null && body.path("paging").path("next").isTextual()) {
            String next = body.path("paging").path("next").asText();
            body = getJson(URI.create(next));
            collectPages(body, pages);
        }
        return pages;
    }

    @Override
    public FacebookPageDetails fetchPageDetails(String pageAccessToken) {
        JsonNode body = getJson(graphUri("/me", "fields", "name,instagram_business_account", "access_token", pageAccessToken));
        if (body == null) {
            return new FacebookPageDetails(null, null);
        }
        String instagramId = body.path("instagram_business_account").path("id").asText(null);
        return new FacebookPageDetails(textOrNull(body.path("name")), instagramId);
    }

    @Override
    public void subscribePage(String pageId, String pageAccessToken) {
        restClient.post()
                .uri(graphUri("/" + pageId + "/subscribed_apps",
                        "access_token", pageAccessToken,
                        "subscribed_fields", SUBSCRIBED_FIELDS))
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
            JsonNode body = parseJson(restClient.post()
                    .uri(graphUri("/me/messages", "access_token", pageAccessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class));
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
            JsonNode body = getJson(graphUri("/" + psid, "fields", "first_name,last_name", "access_token", pageAccessToken));
            if (body == null) {
                return new FacebookUserProfile(null, null);
            }
            return new FacebookUserProfile(textOrNull(body.path("first_name")), textOrNull(body.path("last_name")));
        } catch (RestClientException ex) {
            log.warn("Facebook profile fetch failed for {}: {}", psid, ex.getMessage());
            return new FacebookUserProfile(null, null);
        }
    }

    private String parseAccessToken(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return null;
        }
        String trimmed = responseText.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = jsonMapper.readTree(trimmed);
                String token = node.path("access_token").asText("");
                if (!token.isBlank()) {
                    return token;
                }
            } catch (JacksonException ignored) {
                // Koala falls back to query-string parsing when JSON.parse fails.
            }
        }
        for (String bit : trimmed.split("&")) {
            int eq = bit.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if ("access_token".equals(bit.substring(0, eq))) {
                String value = bit.substring(eq + 1);
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private JsonNode getJson(URI uri) {
        return parseJson(restClient.get().uri(uri).retrieve().body(String.class));
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return jsonMapper.readTree(raw);
    }

    private URI graphUri(String path, String... queryPairs) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.graphBaseUrl() + path);
        for (int i = 0; i + 1 < queryPairs.length; i += 2) {
            builder.queryParam(queryPairs[i], queryPairs[i + 1]);
        }
        return builder.encode().build().toUri();
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
