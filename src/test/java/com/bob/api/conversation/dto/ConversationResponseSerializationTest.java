package com.bob.api.conversation.dto;

import com.bob.api.contact.dto.ContactResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationResponseSerializationTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(
                    JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
            .build();

    @Test
    void usesDisplayIdAsIdAndOmitsAssigneeWhenNull() {
        ConversationResponse response = new ConversationResponse(
                new ConversationResponse.ConversationMetaResponse(
                        new ContactResponse(
                                Map.of(), "offline", "c@example.com", 7, "Customer", null, false, null, "", Map.of(),
                                "contact", null, null
                        ),
                        "Channel::Api",
                        null,
                        null,
                        false
                ),
                12,
                List.of(),
                1,
                "uuid-1",
                Map.of(),
                0L,
                0L,
                true,
                ConversationResponse.ContactInfoRequestResponse.unavailable(),
                0L,
                Map.of(),
                4,
                List.of(),
                false,
                null,
                "open",
                1_705_320_000L,
                1_705_320_000.5d,
                1_705_320_000L,
                0L,
                0L,
                null,
                1_705_320_000L,
                null,
                0L,
                null
        );

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));
        assertThat(json.get("id").asInt()).isEqualTo(12);
        assertThat(json.get("created_at").isIntegralNumber()).isTrue();
        assertThat(json.get("updated_at").isFloatingPointNumber()).isTrue();
        assertThat(json.get("meta").has("assignee")).isFalse();
        assertThat(json.get("meta").has("assignee_type")).isFalse();
        assertThat(json.get("meta").get("sender").get("type").asText()).isEqualTo("contact");
        assertThat(json.get("meta").get("sender").has("created_at")).isFalse();
    }
}
