/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.QuoteResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * One recurring quote in a {@link FeePreview}: a cyclic fee charged while the
 * offer is listed (e.g. a monthly promoted-listing quote).
 *
 * @param name the human-readable quote name
 * @param type the quote type identifier
 * @param feeAmount the fee amount, or {@code null} when the preview carries none
 * @param cycleDuration the billing cycle as an ISO-8601 duration string (e.g.
 *     {@code "P1M"}), or {@code null} when the quote is not recurring
 *
 * @since 0.3.0
 */
public record FeeQuote(
        @Nullable String name,
        @Nullable String type,
        @Nullable Money feeAmount,
        @Nullable String cycleDuration) {

    /**
     * Map the generated quote DTO to the public record.
     *
     * @param raw the generated quote DTO
     * @return the mapped record
     */
    public static FeeQuote from(QuoteResponseRaw raw) {
        return new FeeQuote(
                raw.getName(),
                raw.getType(),
                raw.getFee() == null
                        ? null
                        : Money.of(raw.getFee().getAmount(), raw.getFee().getCurrency()),
                raw.getCycleDuration());
    }
}
