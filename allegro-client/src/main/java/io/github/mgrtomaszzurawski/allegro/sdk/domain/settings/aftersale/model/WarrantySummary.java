/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyBasicRaw;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight warranty entry as returned by the warranties listing. The full
 * definition (periods, attachment, description) is fetched with
 * {@code afterSale().warranty(id)}.
 *
 * @param id warranty definition identifier
 * @param name warranty name, or {@code null} when the server omits it
 *
 * @since 0.2.0
 */
public record WarrantySummary(String id, @Nullable String name) {

    /** Map the generated Layer-1 list-item DTO to the public summary record. */
    public static WarrantySummary from(WarrantyBasicRaw raw) {
        return new WarrantySummary(raw.getId(), raw.getName());
    }
}
