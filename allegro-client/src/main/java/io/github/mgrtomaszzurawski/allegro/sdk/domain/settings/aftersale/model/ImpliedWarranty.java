/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyResponseRaw;
import org.jspecify.annotations.Nullable;

/**
 * An implied-warranty (rękojmia) definition in full, as returned by
 * {@code afterSale().impliedWarranty(id)}, {@code createImpliedWarranty(...)}
 * and {@code updateImpliedWarranty(...)}.
 *
 * @param id implied-warranty definition identifier
 * @param sellerId identifier of the owning seller, or {@code null} when absent
 * @param name implied-warranty name
 * @param individual claim period for individual buyers, or {@code null}
 * @param corporate claim period for corporate buyers, or {@code null}
 * @param address address for buyer claims, or {@code null} when none
 * @param description implied-warranty description, or {@code null} when none
 *
 * @since 0.3.0
 */
public record ImpliedWarranty(
        String id,
        @Nullable String sellerId,
        @Nullable String name,
        @Nullable ImpliedWarrantyPeriod individual,
        @Nullable ImpliedWarrantyPeriod corporate,
        @Nullable AfterSalesAddress address,
        @Nullable String description) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static ImpliedWarranty from(ImpliedWarrantyResponseRaw raw) {
        return new ImpliedWarranty(
                raw.getId().toString(),
                raw.getSeller() == null ? null : raw.getSeller().getId(),
                raw.getName(),
                ImpliedWarrantyPeriod.from(raw.getIndividual()),
                ImpliedWarrantyPeriod.from(raw.getCorporate()),
                raw.getAddress() == null ? null : AfterSalesAddress.from(raw.getAddress()),
                raw.getDescription());
    }
}
