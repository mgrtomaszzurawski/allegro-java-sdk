/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BillingTypeRaw;
import org.jspecify.annotations.Nullable;

/**
 * One entry in the billing-type dictionary (from {@code billing().types()}): the
 * kind of a billing charge and its human-readable description.
 *
 * @param id billing type identifier (matches {@code BillingEntry.typeId()})
 * @param description human-readable description, or {@code null} when absent
 *
 * @since 0.5.0
 */
public record BillingType(String id, @Nullable String description) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static BillingType from(BillingTypeRaw raw) {
        return new BillingType(raw.getId(), raw.getDescription());
    }
}
