/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.disputes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast coverage of the {@link IssueFilter} builder.
 */
class IssueFilterBuilderTest {

    private static final String ORDER_ID = "order-9";

    @Test
    void issueFilter_none_isEmpty() {
        // when
        IssueFilter filter = IssueFilter.none();

        // then
        assertTrue(filter.statuses().isEmpty());
        assertNull(filter.checkoutFormId());
    }

    @Test
    void issueFilter_whenBuilt_carriesStatusesInOrderAndCheckoutForm() {
        // when
        IssueFilter filter = IssueFilter.builder()
                .status(IssueStatus.DISPUTE_ONGOING)
                .status(IssueStatus.CLAIM_SUBMITTED)
                .checkoutFormId(ORDER_ID)
                .build();

        // then
        assertEquals(List.of(IssueStatus.DISPUTE_ONGOING, IssueStatus.CLAIM_SUBMITTED),
                filter.statuses());
        assertEquals(ORDER_ID, filter.checkoutFormId());
    }

    @Test
    void issueFilter_toBuilder_preserves() {
        // given
        IssueFilter original = IssueFilter.builder()
                .status(IssueStatus.DISPUTE_ONGOING)
                .checkoutFormId(ORDER_ID)
                .build();

        // when
        IssueFilter copy = original.toBuilder().build();

        // then
        assertEquals(List.of(IssueStatus.DISPUTE_ONGOING), copy.statuses());
        assertEquals(ORDER_ID, copy.checkoutFormId());
    }

    @Test
    void issueFilter_whenStatusNull_throwsNpe() {
        // then
        assertThrows(NullPointerException.class, () -> IssueFilter.builder().status(null));
    }
}
