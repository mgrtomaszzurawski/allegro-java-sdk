/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyResponseRaw;
import org.jspecify.annotations.Nullable;

/**
 * A seller warranty definition (full detail), as returned by
 * {@code afterSale().warranty(id)}, {@code createWarranty(...)} and
 * {@code updateWarranty(...)}.
 *
 * @param id warranty definition identifier
 * @param sellerId identifier of the owning seller, or {@code null} when absent
 * @param name warranty name
 * @param type who is the warrantor, or {@code null} when the server omits it
 * @param individual warranty duration for individual buyers, or {@code null}
 * @param corporate warranty duration for corporate buyers, or {@code null}
 * @param attachment attached warranty document, or {@code null} when none
 * @param description warranty description, or {@code null} when none
 *
 * @since 0.2.0
 */
public record Warranty(
        String id,
        @Nullable String sellerId,
        @Nullable String name,
        @Nullable WarrantyType type,
        @Nullable WarrantyPeriod individual,
        @Nullable WarrantyPeriod corporate,
        @Nullable AfterSalesAttachment attachment,
        @Nullable String description) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Warranty from(WarrantyResponseRaw raw) {
        return new Warranty(
                raw.getId().toString(),
                raw.getSeller() == null ? null : raw.getSeller().getId(),
                raw.getName(),
                raw.getType() == null ? null : WarrantyType.from(raw.getType()),
                WarrantyPeriod.from(raw.getIndividual()),
                WarrantyPeriod.from(raw.getCorporate()),
                AfterSalesAttachment.from(raw.getAttachment()),
                raw.getDescription());
    }
}
