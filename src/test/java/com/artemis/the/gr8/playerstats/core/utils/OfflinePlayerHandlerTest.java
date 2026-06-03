package com.artemis.the.gr8.playerstats.core.utils;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.artemis.the.gr8.playerstats.MockBukkitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the included/excluded bookkeeping, in particular the UUID-keyed
 * {@code isExcludedPlayer(UUID)} lookup that backs reload filtering and the
 * join listener.
 */
@DisplayName("OfflinePlayerHandler exclude/include bookkeeping")
class OfflinePlayerHandlerTest extends MockBukkitTestBase {

    private OfflinePlayerHandler handler;
    private PlayerMock player;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        handler = OfflinePlayerHandler.getInstance();
        //a freshly added MockBukkit player has not played before, so the
        //JoinListener adds them to the included list on join
        player = server.addPlayer("Tester");
        uuid = player.getUniqueId();
    }

    @Test
    void newlyJoinedPlayerIsIncluded() {
        assertThat(handler.isIncludedPlayer("Tester")).isTrue();
        assertThat(handler.isExcludedPlayer(uuid)).isFalse();
    }

    @Test
    @DisplayName("excluding a player is reflected in the UUID lookup")
    void excludingMovesPlayerToExcludedByUuid() {
        boolean excluded = handler.addPlayerToExcludeList("Tester");

        assertThat(excluded).isTrue();
        assertThat(handler.isExcludedPlayer(uuid)).isTrue();
        assertThat(handler.isExcludedPlayer("Tester")).isTrue();
        assertThat(handler.isIncludedPlayer("Tester")).isFalse();
    }

    @Test
    @DisplayName("re-including a player clears the UUID lookup again")
    void removingFromExcludeListRestoresInclusion() {
        handler.addPlayerToExcludeList("Tester");

        boolean restored = handler.removePlayerFromExcludeList("Tester");

        assertThat(restored).isTrue();
        assertThat(handler.isExcludedPlayer(uuid)).isFalse();
        assertThat(handler.isIncludedPlayer("Tester")).isTrue();
    }
}
