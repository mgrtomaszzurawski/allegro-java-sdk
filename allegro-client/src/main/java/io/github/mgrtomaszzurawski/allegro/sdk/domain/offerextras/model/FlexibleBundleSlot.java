/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FlexibleBundleGetSlotDTORaw;
import java.util.List;
import java.util.Objects;

/**
 * One slot of a flexible bundle — a position a buyer fills by choosing one of
 * the slot's offers.
 *
 * @param id the slot identifier
 * @param order the position of the slot in the bundle (0-based)
 * @param entryPoint whether this slot is the bundle's entry point
 * @param requiredQuantity how many units the slot requires
 * @param offers the offers a buyer may choose for this slot; never {@code null},
 *     possibly empty
 *
 * @since 0.2.0
 */
public record FlexibleBundleSlot(
        String id,
        int order,
        boolean entryPoint,
        int requiredQuantity,
        List<FlexibleBundleSlotOffer> offers) {

    public FlexibleBundleSlot {
        offers = List.copyOf(offers);
    }

    /** Map the generated Layer-1 slot DTO to the public record. */
    static FlexibleBundleSlot from(FlexibleBundleGetSlotDTORaw raw) {
        return new FlexibleBundleSlot(
                Objects.toString(raw.getId(), null),
                raw.getOrder(),
                raw.getEntryPoint(),
                raw.getRequiredQuantity(),
                raw.getOffers() == null
                        ? List.of()
                        : raw.getOffers().stream().map(FlexibleBundleSlotOffer::from).toList());
    }
}
