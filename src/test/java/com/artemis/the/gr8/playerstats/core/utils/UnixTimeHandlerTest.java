package com.artemis.the.gr8.playerstats.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnixTimeHandler")
class UnixTimeHandlerTest {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    @Test
    @DisplayName("a limit of 0 means no limit, so it always returns true")
    void zeroLimitAlwaysTrue() {
        long longAgo = System.currentTimeMillis() - 1000 * DAY_MS;
        assertThat(UnixTimeHandler.hasPlayedSince(0, longAgo)).isTrue();
    }

    @Test
    @DisplayName("returns true when the player played within the limit")
    void withinLimitIsTrue() {
        long threeDaysAgo = System.currentTimeMillis() - 3 * DAY_MS;
        assertThat(UnixTimeHandler.hasPlayedSince(7, threeDaysAgo)).isTrue();
    }

    @Test
    @DisplayName("returns false when the player last played before the limit")
    void beforeLimitIsFalse() {
        long tenDaysAgo = System.currentTimeMillis() - 10 * DAY_MS;
        assertThat(UnixTimeHandler.hasPlayedSince(7, tenDaysAgo)).isFalse();
    }
}
