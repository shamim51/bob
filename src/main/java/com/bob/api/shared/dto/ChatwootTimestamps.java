package com.bob.api.shared.dto;

import java.time.Instant;

public final class ChatwootTimestamps {

    private ChatwootTimestamps() {
    }

    public static long unix(Instant instant) {
        return instant == null ? 0L : instant.getEpochSecond();
    }

    public static double unixFloat(Instant instant) {
        if (instant == null) {
            return 0d;
        }
        return instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
    }
}
