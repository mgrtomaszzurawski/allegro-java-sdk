/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder.BillingFilter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Round-trip tests for the bucket B billing filter builder. */
class BillingBuildersTest {

    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String TYPE_ID = "SALE_COMMISSION";
    private static final String OFFER_ID = "12345";
    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final OffsetDateTime FROM =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO =
            OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void billingFilter_whenAll_isEmpty() {
        // then
        BillingFilter filter = BillingFilter.all();
        assertNull(filter.marketplaceId());
        assertNull(filter.occurredFrom());
        assertNull(filter.occurredTo());
        assertNull(filter.typeId());
        assertNull(filter.offerId());
        assertNull(filter.orderId());
    }

    @Test
    void billingFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        BillingFilter filter = BillingFilter.builder()
                .marketplaceId(MARKETPLACE_ID)
                .occurredFrom(FROM).occurredTo(TO)
                .typeId(TYPE_ID).offerId(OFFER_ID).orderId(ORDER_ID)
                .build();

        // then
        assertEquals(MARKETPLACE_ID, filter.marketplaceId());
        assertEquals(FROM, filter.occurredFrom());
        assertEquals(TO, filter.occurredTo());
        assertEquals(TYPE_ID, filter.typeId());
        assertEquals(OFFER_ID, filter.offerId());
        assertEquals(ORDER_ID, filter.orderId());

        BillingFilter copy = filter.toBuilder().build();
        assertEquals(MARKETPLACE_ID, copy.marketplaceId());
        assertEquals(TYPE_ID, copy.typeId());
        assertEquals(OFFER_ID, copy.offerId());
        assertEquals(ORDER_ID, copy.orderId());
    }
}
