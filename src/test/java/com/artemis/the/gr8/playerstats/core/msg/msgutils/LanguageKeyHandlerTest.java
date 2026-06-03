package com.artemis.the.gr8.playerstats.core.msg.msgutils;

import com.artemis.the.gr8.playerstats.MockBukkitTestBase;
import org.bukkit.Statistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageKeyHandler")
class LanguageKeyHandlerTest extends MockBukkitTestBase {

    private LanguageKeyHandler keyHandler;

    @BeforeEach
    void getHandler() {
        keyHandler = LanguageKeyHandler.getInstance();
    }

    @Nested
    @DisplayName("getStatKey")
    class GetStatKey {

        @Test
        void buildsUntypedStatKey() {
            assertThat(keyHandler.getStatKey(Statistic.JUMP)).isEqualTo("stat.minecraft.jump");
        }

        @Test
        void buildsTypedStatKeyWithRenamedSuffix() {
            //MINE_BLOCK maps to the language suffix "mined"
            assertThat(keyHandler.getStatKey(Statistic.MINE_BLOCK)).isEqualTo("stat_type.minecraft.mined");
        }
    }

    @Nested
    @DisplayName("convertLanguageKeyToDisplayName")
    class ConvertKey {

        @Test
        void translatesKnownStatKeyFromLanguageFile() {
            assertThat(keyHandler.convertLanguageKeyToDisplayName("stat.minecraft.jump")).isEqualTo("Jumps");
        }

        @Test
        @DisplayName("falls back to a prettified name when the key is missing (regression: NPE on unknown stat)")
        void fallsBackToPrettifiedNameForMissingKey() {
            String result = keyHandler.convertLanguageKeyToDisplayName("stat.minecraft.made_up_stat_xyz");

            assertThat(result)
                    .as("missing keys must never return null, which previously crashed serialization")
                    .isNotNull()
                    .isEqualTo("Made Up Stat Xyz");
        }

        @Test
        void prettifiesBlockItemEntitySubKeys() {
            assertThat(keyHandler.convertLanguageKeyToDisplayName("entity.minecraft.zombie")).isEqualTo("Zombie");
        }
    }

    @Nested
    @DisplayName("static key predicates")
    class KeyPredicates {

        @Test
        void recognisesEntityKeys() {
            assertThat(LanguageKeyHandler.isEntityKey("entity.minecraft.zombie")).isTrue();
            assertThat(LanguageKeyHandler.isEntityKey("stat.minecraft.jump")).isFalse();
        }

        @Test
        void recognisesKillEntityAndKilledByKeys() {
            assertThat(LanguageKeyHandler.isNormalKeyForKillEntity("stat_type.minecraft.killed")).isTrue();
            assertThat(LanguageKeyHandler.isNormalKeyForEntityKilledBy("stat_type.minecraft.killed_by")).isTrue();
            assertThat(LanguageKeyHandler.isNormalKeyForKillEntity("stat_type.minecraft.killed_by")).isFalse();
        }
    }
}
