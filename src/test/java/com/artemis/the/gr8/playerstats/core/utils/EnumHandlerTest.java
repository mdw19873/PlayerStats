package com.artemis.the.gr8.playerstats.core.utils;

import com.artemis.the.gr8.playerstats.MockBukkitTestBase;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EnumHandler")
class EnumHandlerTest extends MockBukkitTestBase {

    private EnumHandler enumHandler;

    @BeforeEach
    void getHandler() {
        enumHandler = EnumHandler.getInstance();
    }

    @Nested
    @DisplayName("statistics")
    class Statistics {

        @Test
        void resolvesValidStatisticCaseInsensitively() {
            assertThat(enumHandler.getStatEnum("walk_one_cm")).isEqualTo(Statistic.WALK_ONE_CM);
            assertThat(enumHandler.getStatEnum("WALK_ONE_CM")).isEqualTo(Statistic.WALK_ONE_CM);
        }

        @Test
        void returnsNullForUnknownStatistic() {
            assertThat(enumHandler.getStatEnum("definitely_not_a_stat")).isNull();
        }

        @Test
        void recognisesStatisticNames() {
            assertThat(enumHandler.isStatistic("jump")).isTrue();
            assertThat(enumHandler.isStatistic("definitely_not_a_stat")).isFalse();
        }
    }

    @Nested
    @DisplayName("sub-statistic enums")
    class SubStatEnums {

        @Test
        void resolvesEntityType() {
            assertThat(enumHandler.getEntityEnum("zombie")).isEqualTo(EntityType.ZOMBIE);
            assertThat(enumHandler.getEntityEnum("not_an_entity")).isNull();
        }

        @Test
        void resolvesItemMaterial() {
            assertThat(enumHandler.getItemEnum("diamond")).isEqualTo(Material.DIAMOND);
            assertThat(enumHandler.getItemEnum("not_an_item")).isNull();
        }

        @Test
        void resolvesBlockMaterial() {
            assertThat(enumHandler.getBlockEnum("stone")).isEqualTo(Material.STONE);
            assertThat(enumHandler.getBlockEnum("not_a_block")).isNull();
        }

        @Test
        void recognisesSubStatEntries() {
            assertThat(enumHandler.isSubStatEntry("diamond")).isTrue();
            assertThat(enumHandler.isSubStatEntry("definitely_not_a_substat")).isFalse();
        }
    }
}
