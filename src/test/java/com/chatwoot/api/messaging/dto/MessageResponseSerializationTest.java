package com.chatwoot.api.messaging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageResponseSerializationTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(
                    JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
            .build();

    @Test
    void omitsEchoIdAndSenderWhenNullAndKeepsPrivateKey() {
        MessageResponse response = new MessageResponse(
                9,
                "hello",
                3,
                null,
                42,
                1,
                "text",
                "sent",
                Map.of(),
                1_705_320_000L,
                false,
                null,
                null
        );

        String json = mapper.writeValueAsString(response);
        assertThat(json).contains("\"private\":false");
        assertThat(json).contains("\"conversation_id\":42");
        assertThat(json).contains("\"message_type\":1");
        assertThat(json).doesNotContain("echo_id");
        assertThat(json).doesNotContain("sender");
    }

    @Test
    void includesEchoIdWhenPresent() {
        MessageResponse response = new MessageResponse(
                9,
                "hello",
                3,
                "echo-1",
                42,
                1,
                "text",
                "sent",
                Map.of(),
                1_705_320_000L,
                true,
                "mid-1",
                null
        );

        String json = mapper.writeValueAsString(response);
        assertThat(json).contains("\"echo_id\":\"echo-1\"");
        assertThat(json).contains("\"private\":true");
    }
}
