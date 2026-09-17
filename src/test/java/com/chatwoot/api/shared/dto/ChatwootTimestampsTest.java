package com.chatwoot.api.shared.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ChatwootTimestampsTest {

    @Test
    void unixUsesEpochSecondsAndTreatsNullAsZero() {
        assertThat(ChatwootTimestamps.unix(null)).isZero();
        assertThat(ChatwootTimestamps.unix(Instant.parse("2024-01-15T12:00:00Z"))).isEqualTo(1_705_320_000L);
    }

    @Test
    void unixFloatIncludesFractionalSeconds() {
        Instant instant = Instant.parse("2024-01-15T12:00:00.250Z");
        assertThat(ChatwootTimestamps.unixFloat(null)).isZero();
        assertThat(ChatwootTimestamps.unixFloat(instant)).isEqualTo(1_705_320_000.25d);
    }
}
