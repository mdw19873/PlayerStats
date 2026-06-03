package com.artemis.the.gr8.playerstats.api.enums;

import org.bukkit.Statistic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit")
class UnitTest {

    @Nested
    @DisplayName("fromString")
    class FromString {

        @ParameterizedTest
        @CsvSource({
                "cm, CM",
                "block, BLOCK",
                "blocks, BLOCK",
                "miles, MILE",
                "km, KM",
                "hp, HP",
                "hearts, HEART",
                "days, DAY",
                "hours, HOUR",
                "min, MINUTE",
                "sec, SECOND",
                "ticks, TICK"
        })
        void mapsKnownAliases(String input, Unit expected) {
            assertThat(Unit.fromString(input)).isEqualTo(expected);
        }

        @Test
        void isCaseInsensitive() {
            assertThat(Unit.fromString("CM")).isEqualTo(Unit.CM);
            assertThat(Unit.fromString("Hearts")).isEqualTo(Unit.HEART);
        }

        @Test
        void fallsBackToNumberForUnknown() {
            assertThat(Unit.fromString("nonsense")).isEqualTo(Unit.NUMBER);
        }
    }

    @Nested
    @DisplayName("getTypeFromStatistic")
    class TypeFromStatistic {

        @Test
        void detectsDistanceFromOneCm() {
            assertThat(Unit.getTypeFromStatistic(Statistic.WALK_ONE_CM)).isEqualTo(Unit.Type.DISTANCE);
        }

        @Test
        void detectsDamage() {
            assertThat(Unit.getTypeFromStatistic(Statistic.DAMAGE_DEALT)).isEqualTo(Unit.Type.DAMAGE);
        }

        @Test
        void detectsTime() {
            assertThat(Unit.getTypeFromStatistic(Statistic.PLAY_ONE_MINUTE)).isEqualTo(Unit.Type.TIME);
            assertThat(Unit.getTypeFromStatistic(Statistic.TOTAL_WORLD_TIME)).isEqualTo(Unit.Type.TIME);
        }

        @Test
        void fallsBackToUntyped() {
            assertThat(Unit.getTypeFromStatistic(Statistic.JUMP)).isEqualTo(Unit.Type.UNTYPED);
        }
    }

    @Nested
    @DisplayName("getMostSuitableUnit")
    class MostSuitableUnit {

        @Test
        void picksTimeUnitByMagnitude() {
            assertThat(Unit.getMostSuitableUnit(Unit.Type.TIME, 1_728_000)).isEqualTo(Unit.DAY);   //1 day in ticks
            assertThat(Unit.getMostSuitableUnit(Unit.Type.TIME, 72_000)).isEqualTo(Unit.HOUR);     //1 hour
            assertThat(Unit.getMostSuitableUnit(Unit.Type.TIME, 1_200)).isEqualTo(Unit.MINUTE);    //1 minute
            assertThat(Unit.getMostSuitableUnit(Unit.Type.TIME, 1_000)).isEqualTo(Unit.SECOND);
        }

        @Test
        void picksDistanceUnitByMagnitude() {
            assertThat(Unit.getMostSuitableUnit(Unit.Type.DISTANCE, 100_000)).isEqualTo(Unit.KM);
            assertThat(Unit.getMostSuitableUnit(Unit.Type.DISTANCE, 99_999)).isEqualTo(Unit.BLOCK);
        }

        @Test
        void damageIsAlwaysHeartAndUntypedIsNumber() {
            assertThat(Unit.getMostSuitableUnit(Unit.Type.DAMAGE, 1)).isEqualTo(Unit.HEART);
            assertThat(Unit.getMostSuitableUnit(Unit.Type.UNTYPED, 1)).isEqualTo(Unit.NUMBER);
        }
    }

    @Nested
    @DisplayName("getSmallerUnit")
    class SmallerUnit {

        @Test
        void stepsDownTimeUnits() {
            assertThat(Unit.DAY.getSmallerUnit(1)).isEqualTo(Unit.HOUR);
            assertThat(Unit.DAY.getSmallerUnit(2)).isEqualTo(Unit.MINUTE);
            assertThat(Unit.DAY.getSmallerUnit(3)).isEqualTo(Unit.SECOND);
            assertThat(Unit.HOUR.getSmallerUnit(1)).isEqualTo(Unit.MINUTE);
            assertThat(Unit.MINUTE.getSmallerUnit(1)).isEqualTo(Unit.SECOND);
        }

        @Test
        void stepsDownDistanceAndDamageUnits() {
            assertThat(Unit.KM.getSmallerUnit(1)).isEqualTo(Unit.BLOCK);
            assertThat(Unit.KM.getSmallerUnit(2)).isEqualTo(Unit.CM);
            assertThat(Unit.BLOCK.getSmallerUnit(1)).isEqualTo(Unit.CM);
            assertThat(Unit.HEART.getSmallerUnit(1)).isEqualTo(Unit.HP);
        }

        @Test
        void returnsItselfWhenNoSmallerUnit() {
            assertThat(Unit.SECOND.getSmallerUnit(5)).isEqualTo(Unit.SECOND);
            assertThat(Unit.DAY.getSmallerUnit(0)).isEqualTo(Unit.DAY);
        }
    }

    @Nested
    @DisplayName("conversions and labels")
    class ConversionsAndLabels {

        @Test
        void getSecondsReturnsTimeValueOrMinusOne() {
            assertThat(Unit.DAY.getSeconds()).isEqualTo(86_400);
            assertThat(Unit.HOUR.getSeconds()).isEqualTo(3_600);
            assertThat(Unit.MINUTE.getSeconds()).isEqualTo(60);
            assertThat(Unit.SECOND.getSeconds()).isEqualTo(1);
            assertThat(Unit.TICK.getSeconds()).isEqualTo(1 / 20.0);
            assertThat(Unit.KM.getSeconds()).isEqualTo(-1); //not a time unit
        }

        @Test
        void getShortLabelReturnsTimeCharOrQuestionMark() {
            assertThat(Unit.DAY.getShortLabel()).isEqualTo('d');
            assertThat(Unit.HOUR.getShortLabel()).isEqualTo('h');
            assertThat(Unit.MINUTE.getShortLabel()).isEqualTo('m');
            assertThat(Unit.SECOND.getShortLabel()).isEqualTo('s');
            assertThat(Unit.CM.getShortLabel()).isEqualTo('?');
        }

        @Test
        void exposesLabelAndType() {
            assertThat(Unit.NUMBER.getLabel()).isEqualTo("Times");
            assertThat(Unit.KM.getType()).isEqualTo(Unit.Type.DISTANCE);
            assertThat(Unit.HEART.getType()).isEqualTo(Unit.Type.DAMAGE);
        }
    }
}
