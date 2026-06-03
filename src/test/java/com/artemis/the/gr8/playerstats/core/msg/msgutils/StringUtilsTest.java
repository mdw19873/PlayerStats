package com.artemis.the.gr8.playerstats.core.msg.msgutils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringUtils.prettify")
class StringUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "walk_one_cm, Walk One Cm",
            "damage_dealt, Damage Dealt",
            "multiple_word_enum_name, Multiple Word Enum Name",
            "jump, Jump"
    })
    void replacesUnderscoresAndCapitalisesEachWord(String input, String expected) {
        assertThat(StringUtils.prettify(input)).isEqualTo(expected);
    }

    @Test
    void normalisesMixedCaseInput() {
        assertThat(StringUtils.prettify("WALK_ONE_CM")).isEqualTo("Walk One Cm");
    }

    @Test
    void capitalisesSingleWord() {
        assertThat(StringUtils.prettify("a")).isEqualTo("A");
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(StringUtils.prettify(null)).isNull();
    }
}
