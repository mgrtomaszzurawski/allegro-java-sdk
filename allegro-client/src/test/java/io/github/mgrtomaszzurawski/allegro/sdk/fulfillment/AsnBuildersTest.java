/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.SubmittedAsnUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.HandlingUnit;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast proof for the Advance Ship Notice request builders
 * ({@link AsnRequest}, {@link SubmittedAsnUpdate}, {@link AsnFilter}) — every
 * builder method is exercised, and the spec's quantity bounds and the
 * at-least-one-item rule are enforced before any request is built.
 */
class AsnBuildersTest {

    private static final String PRODUCT_ID = "11111111-2222-3333-4444-555555555555";
    private static final String OTHER_PRODUCT_ID = "99999999-8888-7777-6666-555555555555";
    private static final int QUANTITY = 5;
    private static final int OTHER_QUANTITY = 7;
    private static final int MAX_QUANTITY = 1_000_000;
    private static final int OVER_MAX_QUANTITY = 1_000_001;
    private static final int ZERO_QUANTITY = 0;
    private static final String UNIT_TYPE = "BOX";
    private static final String LABELS_TYPE = "NONE";
    private static final BigDecimal HANDLING_AMOUNT = BigDecimal.valueOf(3);
    private static final BigDecimal VOLUME = BigDecimal.valueOf(12_000);

    private static HandlingUnit sampleHandlingUnit() {
        return new HandlingUnit(UNIT_TYPE, HANDLING_AMOUNT, LABELS_TYPE);
    }

    // ---- AsnRequest ----

    @Test
    void asnRequest_whenAllFieldsSet_roundTripsThroughToBuilder() {
        // given
        AsnRequest request = AsnRequest.builder()
                .addItem(PRODUCT_ID, QUANTITY)
                .handlingUnit(sampleHandlingUnit())
                .declaredVolumeInCc(VOLUME)
                .build();

        // when
        AsnRequest copy = request.toBuilder().build();

        // then
        assertEquals(1, copy.items().size());
        assertEquals(PRODUCT_ID, copy.items().get(0).productId());
        assertEquals(BigDecimal.valueOf(QUANTITY), copy.items().get(0).quantity());
        assertEquals(UNIT_TYPE, copy.handlingUnit().unitType());
        assertEquals(VOLUME, copy.declaredVolumeInCc());
    }

    @Test
    void asnRequest_whenItemsSetterUsed_replacesLines() {
        // given a builder that already has a line, then the setter is applied
        // when
        AsnRequest request = AsnRequest.builder()
                .addItem(PRODUCT_ID, QUANTITY)
                .items(List.of(new AsnItem(OTHER_PRODUCT_ID, BigDecimal.valueOf(OTHER_QUANTITY))))
                .build();

        // then — the setter replaced the earlier line rather than appending
        assertEquals(1, request.items().size());
        assertEquals(OTHER_PRODUCT_ID, request.items().get(0).productId());
        assertEquals(BigDecimal.valueOf(OTHER_QUANTITY), request.items().get(0).quantity());
    }

    @Test
    void asnRequest_whenOnlyRequiredItem_buildsWithoutOptionalFields() {
        // given / when — the minimal valid request: one item, nothing optional
        AsnRequest request = AsnRequest.builder().addItem(PRODUCT_ID, QUANTITY).build();

        // then
        assertEquals(1, request.items().size());
        assertNull(request.handlingUnit());
        assertNull(request.declaredVolumeInCc());
    }

    @Test
    void asnRequest_whenProductIdNotUuid_throws() {
        // given
        AsnRequest.Builder builder = AsnRequest.builder();
        // then — a non-UUID product id fails fast at the builder
        assertThrows(IllegalArgumentException.class, () -> builder.addItem("not-a-uuid", QUANTITY));
    }

    @Test
    void asnRequest_whenNoItem_throwsOnBuild() {
        // given
        AsnRequest.Builder builder = AsnRequest.builder();
        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void asnRequest_whenProductIdBlank_throws() {
        // given
        AsnRequest.Builder builder = AsnRequest.builder();
        // then
        assertThrows(IllegalArgumentException.class, () -> builder.addItem("  ", QUANTITY));
    }

    @Test
    void asnRequest_whenQuantityBelowMinimum_throws() {
        // given
        AsnRequest.Builder builder = AsnRequest.builder();
        // then
        assertThrows(IllegalArgumentException.class, () -> builder.addItem(PRODUCT_ID, ZERO_QUANTITY));
    }

    @Test
    void asnRequest_whenQuantityAboveMaximum_throws() {
        // given
        AsnRequest.Builder builder = AsnRequest.builder();
        // then
        assertThrows(IllegalArgumentException.class, () -> builder.addItem(PRODUCT_ID, OVER_MAX_QUANTITY));
    }

    @Test
    void asnRequest_whenQuantityAtMaximum_isAccepted() {
        // given / when
        AsnRequest request = AsnRequest.builder().addItem(PRODUCT_ID, MAX_QUANTITY).build();
        // then — the boundary value is valid
        assertEquals(BigDecimal.valueOf(MAX_QUANTITY), request.items().get(0).quantity());
    }

    // ---- SubmittedAsnUpdate ----

    @Test
    void submittedUpdate_whenAllFieldsSet_roundTripsThroughToBuilder() {
        // given
        SubmittedAsnUpdate update = SubmittedAsnUpdate.builder()
                .addItem(PRODUCT_ID, QUANTITY)
                .handlingUnit(sampleHandlingUnit())
                .declaredVolumeInCc(VOLUME)
                .build();

        // when
        SubmittedAsnUpdate copy = update.toBuilder().build();

        // then
        assertEquals(PRODUCT_ID, copy.items().get(0).productId());
        assertEquals(UNIT_TYPE, copy.handlingUnit().unitType());
        assertEquals(VOLUME, copy.declaredVolumeInCc());
    }

    @Test
    void submittedUpdate_whenEmpty_hasNoLines() {
        // given / when
        SubmittedAsnUpdate update = SubmittedAsnUpdate.builder().build();
        // then — an empty update touches nothing
        assertTrue(update.items().isEmpty());
    }

    @Test
    void submittedUpdate_whenItemsSetterUsed_replacesLines() {
        // given a builder that already has a line, then the setter is applied
        // when
        SubmittedAsnUpdate update = SubmittedAsnUpdate.builder()
                .addItem(PRODUCT_ID, QUANTITY)
                .items(List.of(new AsnItem(OTHER_PRODUCT_ID, BigDecimal.valueOf(OTHER_QUANTITY))))
                .build();
        // then — the setter replaced the earlier line rather than appending
        assertEquals(1, update.items().size());
        assertEquals(OTHER_PRODUCT_ID, update.items().get(0).productId());
    }

    @Test
    void submittedUpdate_whenQuantityOutOfRange_throws() {
        // given
        SubmittedAsnUpdate.Builder builder = SubmittedAsnUpdate.builder();
        // then
        assertThrows(IllegalArgumentException.class, () -> builder.addItem(PRODUCT_ID, OVER_MAX_QUANTITY));
    }

    // ---- AsnFilter ----

    @Test
    void asnFilter_whenStatusesAdded_roundTripsThroughToBuilder() {
        // given
        AsnFilter filter = AsnFilter.builder()
                .addStatus(AsnStatus.DRAFT)
                .addStatus(AsnStatus.IN_TRANSIT)
                .build();

        // when
        AsnFilter copy = filter.toBuilder().build();

        // then
        assertEquals(List.of(AsnStatus.DRAFT, AsnStatus.IN_TRANSIT), copy.statuses());
    }

    @Test
    void asnFilter_whenStatusesSetterUsed_replacesStatuses() {
        // given / when
        AsnFilter filter = AsnFilter.builder()
                .addStatus(AsnStatus.DRAFT)
                .statuses(List.of(AsnStatus.COMPLETED))
                .build();
        // then — the setter replaced, not appended
        assertEquals(List.of(AsnStatus.COMPLETED), filter.statuses());
    }

    @Test
    void asnFilter_all_hasNoStatuses() {
        // given / when
        AsnFilter filter = AsnFilter.all();
        // then
        assertTrue(filter.statuses().isEmpty());
    }
}
