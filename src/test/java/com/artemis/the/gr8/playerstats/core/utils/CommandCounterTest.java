package com.artemis.the.gr8.playerstats.core.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommandCounter")
class CommandCounterTest {

    private CommandCounter counter;

    @BeforeEach
    void setUp() {
        counter = CommandCounter.getInstance();
        counter.getCommandCounts(); //draining resets all counts to 0
    }

    @Test
    void getInstanceReturnsSingleton() {
        assertThat(CommandCounter.getInstance()).isSameAs(CommandCounter.getInstance());
    }

    @Test
    void countsAccumulatePerCategory() {
        counter.upHelpCommandCount();
        counter.upHelpCommandCount();
        counter.upTopStatCommandCount();

        Map<String, Integer> counts = counter.getCommandCounts();
        assertThat(counts).containsEntry("Help", 2);
        assertThat(counts).containsEntry("Top Stat", 1);
        assertThat(counts).containsEntry("Server Stat", 0);
    }

    @Test
    void eachIncrementMapsToTheRightCategory() {
        counter.upExcludeCommandCount();
        counter.upShareCommandCount();
        counter.upPlayerStatCommandCount();
        counter.upServerStatCommandCount();

        assertThat(counter.getCommandCounts())
                .containsEntry("Exclude", 1)
                .containsEntry("Share", 1)
                .containsEntry("Player Stat", 1)
                .containsEntry("Server Stat", 1);
    }

    @Test
    @DisplayName("getCommandCounts resets the counters after reading")
    void readingResetsCounters() {
        counter.upHelpCommandCount();
        counter.getCommandCounts(); //drains

        assertThat(counter.getCommandCounts()).containsEntry("Help", 0);
    }
}
