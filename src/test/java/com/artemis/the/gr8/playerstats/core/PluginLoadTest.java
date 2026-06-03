package com.artemis.the.gr8.playerstats.core;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Plugin load (MockBukkit smoke test)")
class PluginLoadTest {

    private ServerMock server;
    private Main plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Main.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginEnablesCleanly() {
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void registersTheStatisticCommand() {
        assertThat(server.getPluginCommand("statistic")).isNotNull();
    }
}
