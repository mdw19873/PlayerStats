package com.artemis.the.gr8.playerstats.core.commands;

import com.artemis.the.gr8.playerstats.MockBukkitTestBase;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.api.enums.Target;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatCommand argument parsing")
class StatCommandTest extends MockBukkitTestBase {

    private StatCommand statCommand;
    private CommandSender console;

    @BeforeEach
    void getCommand() {
        //the registered executor is the StatCommand instance the plugin wired up
        statCommand = (StatCommand) server.getPluginCommand("statistic").getExecutor();
        console = server.getConsoleSender();
    }

    private StatRequest<?> parse(String... args) {
        return statCommand.buildStatRequest(console, args);
    }

    @Nested
    @DisplayName("target detection")
    class TargetDetection {

        @Test
        void detectsTopTarget() {
            StatRequest<?> request = parse("jump", "top");
            assertThat(request).isNotNull();
            assertThat(request.getSettings().getTarget()).isEqualTo(Target.TOP);
            assertThat(request.getSettings().getStatistic()).isEqualTo(Statistic.JUMP);
        }

        @Test
        void detectsServerTarget() {
            StatRequest<?> request = parse("jump", "server");
            assertThat(request).isNotNull();
            assertThat(request.getSettings().getTarget()).isEqualTo(Target.SERVER);
        }

        @Test
        void defaultsToTopWhenNoTargetGiven() {
            StatRequest<?> request = parse("jump");
            assertThat(request).isNotNull();
            assertThat(request.getSettings().getTarget()).isEqualTo(Target.TOP);
        }
    }

    @Nested
    @DisplayName("sub-statistic parsing")
    class SubStatisticParsing {

        @Test
        void parsesBlockSubStatistic() {
            StatRequest<?> request = parse("mine_block", "stone", "top");
            assertThat(request).isNotNull();
            assertThat(request.getSettings().getStatistic()).isEqualTo(Statistic.MINE_BLOCK);
            assertThat(request.getSettings().getBlock()).isEqualTo(Material.STONE);
        }

        @Test
        void parsesEntitySubStatistic() {
            StatRequest<?> request = parse("kill_entity", "zombie", "top");
            assertThat(request).isNotNull();
            assertThat(request.getSettings().getStatistic()).isEqualTo(Statistic.KILL_ENTITY);
            assertThat(request.getSettings().getEntity()).isEqualTo(EntityType.ZOMBIE);
        }
    }

    @Nested
    @DisplayName("invalid input")
    class InvalidInput {

        @Test
        void returnsNullForUnknownStatistic() {
            assertThat(parse("not_a_real_stat", "top")).isNull();
        }

        @Test
        void returnsNullForTypedStatisticWithoutSubStat() {
            //MINE_BLOCK needs a block; without one no valid request can be built
            assertThat(parse("mine_block", "top")).isNull();
        }
    }
}
