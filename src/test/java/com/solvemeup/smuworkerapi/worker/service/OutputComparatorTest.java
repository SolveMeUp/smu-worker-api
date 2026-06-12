package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputComparatorTest {

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void comparesJsonStructurally() {
        assertThat(comparator.compare("[[1,2],[3,4]]", "[ [1, 2], [3, 4] ]")).isTrue();
        assertThat(comparator.compare("\"hello  world\"", "\"hello world\"")).isFalse();
    }

    @Test
    void comparesServerOutputWithCoreJsonByReturnType() {
        assertThat(comparator.compare("1 2 3", "[1,2,3]", ValueType.INT_ARRAY)).isTrue();
        assertThat(comparator.compare("*\n**\n***", "\"*\\n**\\n***\\n\"", ValueType.STRING)).isTrue();
        assertThat(comparator.compare(
                "2 2\nChongChong Alice\nAlice Bob",
                "[[\"ChongChong\",\"Alice\"],[\"Alice\",\"Bob\"]]",
                ValueType.STRING_2D_ARRAY
        )).isTrue();
    }

    @Test
    void fallsBackToLegacyTextComparison() {
        assertThat(comparator.compare("1  2\n3", "1 2\n3")).isTrue();
    }

    @Test
    void preservesSpacesForTextReturnTypes() {
        assertThat(comparator.compare("hello  world", "hello world", ValueType.STRING)).isFalse();
        assertThat(comparator.compare("hello  world", "\"hello  world\"", ValueType.STRING)).isTrue();
    }
}
