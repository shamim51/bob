package com.bob.api.integration.facebook.service;

import tools.jackson.databind.JsonNode;

public final class FacebookMessagingEvent {

    private final JsonNode messaging;

    public FacebookMessagingEvent(JsonNode messaging) {
        this.messaging = messaging;
    }

    public String senderId() {
        return text(messaging.path("sender").path("id"));
    }

    public String recipientId() {
        return text(messaging.path("recipient").path("id"));
    }

    public String content() {
        String text = text(messaging.path("message").path("text"));
        if (text != null) {
            return text;
        }
        String title = text(messaging.path("postback").path("title"));
        if (title != null) {
            return title;
        }
        String payload = text(messaging.path("postback").path("payload"));
        if (payload != null) {
            return payload;
        }
        JsonNode attachments = messaging.path("message").path("attachments");
        if (attachments.isArray() && !attachments.isEmpty()) {
            JsonNode first = attachments.get(0);
            String type = text(first.path("type"));
            String url = text(first.path("payload").path("url"));
            if (url != null) {
                return url;
            }
            return type == null ? "[Attachment]" : "[" + type + "]";
        }
        return null;
    }

    public String identifier() {
        String mid = text(messaging.path("message").path("mid"));
        if (mid != null) {
            return mid;
        }
        return text(messaging.path("postback").path("mid"));
    }

    public String appId() {
        JsonNode appId = messaging.path("message").path("app_id");
        if (appId.isMissingNode() || appId.isNull()) {
            return null;
        }
        return appId.asText();
    }

    public boolean echo() {
        return messaging.path("message").path("is_echo").asBoolean(false);
    }

    public boolean deliveryOrRead() {
        return messaging.has("delivery") || messaging.has("read");
    }

    public String inReplyToExternalId() {
        return text(messaging.path("message").path("reply_to").path("mid"));
    }

    public boolean sentFromApp(String appId) {
        String eventAppId = appId();
        return eventAppId != null && appId != null && eventAppId.equals(appId);
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }
}
