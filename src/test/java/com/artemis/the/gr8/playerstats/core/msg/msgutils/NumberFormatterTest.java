package com.artemis.the.gr8.playerstats.core.msg.msgutils;

import com.artemis.the.gr8.playerstats.api.enums.Unit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NumberFormatter")
class NumberFormatterTest {

    private static Locale originalLocale;
    private NumberFormatter formatter;

    @BeforeAll
    static void pinLocale() {
        //formatDefault/Damage/Distance use DecimalFormat, whose grouping separator
        //is locale-dependent. Pin US so assertions ("1,234") are deterministic.
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterAll
    static void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @BeforeEach
    void setUp() {
        formatter = new NumberFormatter();
    }

    @Nested
    @DisplayName("formatDefaultNumber")
    class DefaultNumber {

        @Test
        void groupsThousandsWithCommas() {
            assertThat(formatter.formatDefaultNumber(1_234_567)).isEqualTo("1,234,567");
        }

        @Test
        void leavesSmallNumbersUnchanged() {
            assertThat(formatter.formatDefaultNumber(42)).isEqualTo("42");
        }

        @Test
        void handlesZero() {
            assertThat(formatter.formatDefaultNumber(0)).isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("formatDamageNumber")
    class DamageNumber {

        @Test
        void halvesAndRoundsForHearts() {
            //damage stats are stored in half-hearts; HEART converts to whole hearts
            assertThat(formatter.formatDamageNumber(10, Unit.HEART)).isEqualTo("5");
            assertThat(formatter.formatDamageNumber(11, Unit.HEART)).isEqualTo("6"); //round(5.5)
        }

        @Test
        void leavesValueUnchangedForHp() {
            assertThat(formatter.formatDamageNumber(10, Unit.HP)).isEqualTo("10");
        }
    }

    @Nested
    @DisplayName("formatDistanceNumber")
    class DistanceNumber {

        @Test
        void keepsCentimetresAsIs() {
            assertThat(formatter.formatDistanceNumber(1500, Unit.CM)).isEqualTo("1,500");
        }

        @Test
        void convertsCentimetresToBlocks() {
            //default branch divides by 100
            assertThat(formatter.formatDistanceNumber(500, Unit.BLOCK)).isEqualTo("5");
        }

        @Test
        void convertsCentimetresToKilometres() {
            assertThat(formatter.formatDistanceNumber(100_000, Unit.KM)).isEqualTo("1");
        }

        @Test
        void convertsCentimetresToMiles() {
            assertThat(formatter.formatDistanceNumber(160_934, Unit.MILE)).isEqualTo("1");
        }
    }

    @Nested
    @DisplayName("formatTimeNumber")
    class TimeNumber {

        @Test
        void returnsDashForZeroOrLess() {
            assertThat(formatter.formatTimeNumber(0, Unit.DAY, Unit.SECOND)).isEqualTo("-");
            assertThat(formatter.formatTimeNumber(-5, Unit.DAY, Unit.SECOND)).isEqualTo("-");
        }

        @Test
        void passesThroughWhenUnitsAreTicksOnlyOrNumber() {
            assertThat(formatter.formatTimeNumber(20, Unit.TICK, Unit.TICK)).isEqualTo("20");
            assertThat(formatter.formatTimeNumber(20, Unit.NUMBER, Unit.SECOND)).isEqualTo("20");
        }

        @Test
        void formatsExactMinute() {
            //1200 ticks = 60 seconds = 1 minute
            assertThat(formatter.formatTimeNumber(1200, Unit.MINUTE, Unit.SECOND)).isEqualTo("1m");
        }

        @Test
        void formatsExactDay() {
            //1,728,000 ticks = 86,400 seconds = 1 day
            assertThat(formatter.formatTimeNumber(1_728_000, Unit.DAY, Unit.HOUR)).isEqualTo("1d");
        }

        @Test
        void rollsUpAcrossMultipleUnits() {
            //73,220 ticks = 3,661 seconds = 1h 1m 1s
            assertThat(formatter.formatTimeNumber(73_220, Unit.HOUR, Unit.SECOND)).isEqualTo("1h 1m 1s");
        }
    }
}
