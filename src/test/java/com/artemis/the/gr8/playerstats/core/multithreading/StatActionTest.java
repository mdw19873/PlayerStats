package com.artemis.the.gr8.playerstats.core.multithreading;

import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;
import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the fork/join statistic computation. {@link StatAction} reaches the
 * {@link OfflinePlayerHandler} singleton from worker threads, so we inject a
 * Mockito mock into its real static {@code instance} field (rather than using
 * a thread-local {@code mockStatic}) so the stub is visible on every worker
 * thread. No MockBukkit server is needed: the only Bukkit types involved are
 * the (mocked) OfflinePlayer and the Statistic/Material/EntityType enums.
 */
@DisplayName("StatAction (fork/join stat computation)")
class StatActionTest {

    private OfflinePlayerHandler handlerMock;

    @BeforeEach
    void injectHandlerSingleton() throws Exception {
        handlerMock = mock(OfflinePlayerHandler.class);
        setSingletonInstance(handlerMock);
    }

    @AfterEach
    void clearHandlerSingleton() throws Exception {
        setSingletonInstance(null);
    }

    private static void setSingletonInstance(OfflinePlayerHandler value) throws Exception {
        Field instanceField = OfflinePlayerHandler.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, value);
    }

    private ConcurrentHashMap<String, Integer> compute(List<String> names, StatRequest.Settings settings) {
        ConcurrentHashMap<String, Integer> result = new ConcurrentHashMap<>();
        StatAction action = new StatAction(ImmutableList.copyOf(names), settings, result);
        return ForkJoinPool.commonPool().invoke(action);
    }

    private StatRequest.Settings untypedSettings(Statistic statistic) {
        StatRequest.Settings settings = mock(StatRequest.Settings.class);
        when(settings.getStatistic()).thenReturn(statistic);
        return settings;
    }

    private void stubPlayer(String name, OfflinePlayer player) {
        when(handlerMock.getIncludedOfflinePlayer(name)).thenReturn(player);
    }

    @Nested
    @DisplayName("aggregation")
    class Aggregation {

        @Test
        void sumsUntypedStatisticsPerPlayer() {
            StatRequest.Settings settings = untypedSettings(Statistic.JUMP);
            OfflinePlayer alice = mock(OfflinePlayer.class);
            OfflinePlayer bob = mock(OfflinePlayer.class);
            when(alice.getStatistic(Statistic.JUMP)).thenReturn(5);
            when(bob.getStatistic(Statistic.JUMP)).thenReturn(12);
            stubPlayer("Alice", alice);
            stubPlayer("Bob", bob);

            ConcurrentHashMap<String, Integer> result = compute(List.of("Alice", "Bob"), settings);

            assertThat(result).containsEntry("Alice", 5).containsEntry("Bob", 12);
        }

        @Test
        @DisplayName("only players with a value above zero are included")
        void excludesZeroValues() {
            StatRequest.Settings settings = untypedSettings(Statistic.JUMP);
            OfflinePlayer zero = mock(OfflinePlayer.class);
            OfflinePlayer positive = mock(OfflinePlayer.class);
            when(zero.getStatistic(Statistic.JUMP)).thenReturn(0);
            when(positive.getStatistic(Statistic.JUMP)).thenReturn(3);
            stubPlayer("Zero", zero);
            stubPlayer("Positive", positive);

            ConcurrentHashMap<String, Integer> result = compute(List.of("Zero", "Positive"), settings);

            assertThat(result).doesNotContainKey("Zero").containsEntry("Positive", 3);
        }

        @Test
        void emptyPlayerListYieldsEmptyMap() {
            ConcurrentHashMap<String, Integer> result = compute(List.of(), untypedSettings(Statistic.JUMP));
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("sub-statistic types")
    class SubStatisticTypes {

        @Test
        void readsEntityStatistic() {
            StatRequest.Settings settings = mock(StatRequest.Settings.class);
            when(settings.getStatistic()).thenReturn(Statistic.KILL_ENTITY);
            when(settings.getEntity()).thenReturn(EntityType.ZOMBIE);
            OfflinePlayer hunter = mock(OfflinePlayer.class);
            when(hunter.getStatistic(Statistic.KILL_ENTITY, EntityType.ZOMBIE)).thenReturn(7);
            stubPlayer("Hunter", hunter);

            assertThat(compute(List.of("Hunter"), settings)).containsEntry("Hunter", 7);
        }

        @Test
        void readsBlockStatistic() {
            StatRequest.Settings settings = mock(StatRequest.Settings.class);
            when(settings.getStatistic()).thenReturn(Statistic.MINE_BLOCK);
            when(settings.getBlock()).thenReturn(Material.STONE);
            OfflinePlayer miner = mock(OfflinePlayer.class);
            when(miner.getStatistic(Statistic.MINE_BLOCK, Material.STONE)).thenReturn(99);
            stubPlayer("Miner", miner);

            assertThat(compute(List.of("Miner"), settings)).containsEntry("Miner", 99);
        }

        @Test
        void readsItemStatistic() {
            StatRequest.Settings settings = mock(StatRequest.Settings.class);
            when(settings.getStatistic()).thenReturn(Statistic.BREAK_ITEM);
            when(settings.getItem()).thenReturn(Material.DIAMOND_PICKAXE);
            OfflinePlayer breaker = mock(OfflinePlayer.class);
            when(breaker.getStatistic(Statistic.BREAK_ITEM, Material.DIAMOND_PICKAXE)).thenReturn(4);
            stubPlayer("Breaker", breaker);

            assertThat(compute(List.of("Breaker"), settings)).containsEntry("Breaker", 4);
        }
    }

    @Nested
    @DisplayName("recursive splitting")
    class RecursiveSplitting {

        @Test
        @DisplayName("a list past the split threshold still aggregates every player")
        void splitsLargeListAndAggregatesAll() {
            StatRequest.Settings settings = untypedSettings(Statistic.JUMP);
            int playerCount = 25; //well above the threshold of 10, forcing several recursive splits
            List<String> names = new ArrayList<>();
            for (int i = 0; i < playerCount; i++) {
                String name = "Player" + i;
                names.add(name);
                OfflinePlayer player = mock(OfflinePlayer.class);
                when(player.getStatistic(Statistic.JUMP)).thenReturn(i + 1);
                stubPlayer(name, player);
            }

            ConcurrentHashMap<String, Integer> result = compute(names, settings);

            assertThat(result).hasSize(playerCount);
            assertThat(result).containsEntry("Player0", 1).containsEntry("Player24", 25);
        }
    }
}
