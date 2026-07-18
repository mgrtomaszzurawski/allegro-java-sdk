/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingAnswer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingRemoval;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Round-trip and validation tests for the bucket D rating builders. */
class RatingBuildersTest {

    private static final String MESSAGE = "Thank you for your feedback";
    private static final OffsetDateTime CHANGED_FROM =
            OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime CHANGED_TO =
            OffsetDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void ratingAnswer_whenMessageSet_buildsAndToBuilderPreserves() {
        // when
        RatingAnswer answer = RatingAnswer.builder().message(MESSAGE).build();

        // then
        assertEquals(MESSAGE, answer.message());
        assertEquals(MESSAGE, answer.toBuilder().build().message());
    }

    @Test
    void ratingAnswer_whenMessageMissing_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class, () -> RatingAnswer.builder().build());
    }

    @Test
    void ratingAnswer_whenMessageTooLong_throwsIllegalState() {
        // given
        String tooLong = "a".repeat(RatingAnswer.MAX_MESSAGE_LENGTH + 1);

        // then
        assertThrows(IllegalStateException.class,
                () -> RatingAnswer.builder().message(tooLong).build());
    }

    @Test
    void ratingAnswer_whenMessageAtMaxLength_builds() {
        // given
        String atMax = "a".repeat(RatingAnswer.MAX_MESSAGE_LENGTH);

        // when
        RatingAnswer answer = RatingAnswer.builder().message(atMax).build();

        // then
        assertEquals(RatingAnswer.MAX_MESSAGE_LENGTH, answer.message().length());
    }

    @Test
    void ratingRemoval_whenMessageSet_buildsAndToBuilderPreserves() {
        // when
        RatingRemoval removal = RatingRemoval.builder().message(MESSAGE).build();

        // then
        assertEquals(MESSAGE, removal.message());
        assertEquals(MESSAGE, removal.toBuilder().build().message());
    }

    @Test
    void ratingRemoval_whenMessageBlank_throwsIllegalState() {
        // then
        assertThrows(IllegalStateException.class,
                () -> RatingRemoval.builder().message(" ").build());
    }

    @Test
    void ratingRemoval_whenMessageTooLong_throwsIllegalState() {
        // given
        String tooLong = "a".repeat(RatingRemoval.MAX_MESSAGE_LENGTH + 1);

        // then
        assertThrows(IllegalStateException.class,
                () -> RatingRemoval.builder().message(tooLong).build());
    }

    @Test
    void ratingFilter_whenAll_hasNoCriteria() {
        // when
        RatingFilter filter = RatingFilter.all();

        // then
        assertNull(filter.recommended());
        assertNull(filter.changedFrom());
        assertNull(filter.changedTo());
    }

    @Test
    void ratingFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        RatingFilter filter = RatingFilter.builder()
                .recommended(false)
                .changedFrom(CHANGED_FROM)
                .changedTo(CHANGED_TO)
                .build();

        // then
        assertFalse(filter.recommended());
        assertEquals(CHANGED_FROM, filter.changedFrom());
        assertEquals(CHANGED_TO, filter.changedTo());

        RatingFilter copy = filter.toBuilder().build();
        assertEquals(CHANGED_FROM, copy.changedFrom());
        assertEquals(CHANGED_TO, copy.changedTo());
        assertFalse(copy.recommended());
    }
}
