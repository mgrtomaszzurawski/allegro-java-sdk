/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyBasicRaw;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight implied-warranty entry as returned by the listing. The full
 * definition (periods, address, description) is fetched with
 * {@code afterSale().impliedWarranty(id)}.
 *
 * @param id implied-warranty definition identifier
 * @param name implied-warranty name, or {@code null} when the server omits it
 *
 * @since 0.3.0
 */
public record ImpliedWarrantySummary(String id, @Nullable String name) {

    /** Map the generated Layer-1 list-item DTO to the public summary record. */
    public static ImpliedWarrantySummary from(ImpliedWarrantyBasicRaw raw) {
        return new ImpliedWarrantySummary(raw.getId(), raw.getName());
    }
}
