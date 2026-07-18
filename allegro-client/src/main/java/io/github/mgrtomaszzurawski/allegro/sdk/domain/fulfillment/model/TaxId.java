/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TaxIdResponseRaw;
import org.jspecify.annotations.Nullable;

/**
 * The seller's tax identification number registered for One Fulfillment, with
 * Allegro's verification status for it. Read via {@code fulfillment().taxId()};
 * set via {@code fulfillment().addTaxId(...)} / {@code updateTaxId(...)}.
 *
 * @param taxId              the tax identification number
 * @param verificationStatus Allegro's verification status for the number
 *
 * @since 0.3.0
 */
public record TaxId(
        @Nullable String taxId,
        @Nullable String verificationStatus) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static TaxId from(TaxIdResponseRaw raw) {
        return new TaxId(raw.getTaxId(), raw.getVerificationStatus());
    }
}
