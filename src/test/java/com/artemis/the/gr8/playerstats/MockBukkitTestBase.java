package com.artemis.the.gr8.playerstats;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.artemis.the.gr8.playerstats.core.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for server-backed tests. Loads the PlayerStats plugin into a
 * simulated MockBukkit server before each test and tears it down afterwards,
 * so the plugin's singleton managers (EnumHandler, LanguageKeyHandler,
 * OfflinePlayerHandler, ...) are fully initialised and usable.
 */
public abstract class MockBukkitTestBase {

    protected ServerMock server;
    protected Main plugin;

    @BeforeEach
    void loadPlugin() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Main.class);
    }

    @AfterEach
    void unloadPlugin() {
        MockBukkit.unmock();
    }
}
