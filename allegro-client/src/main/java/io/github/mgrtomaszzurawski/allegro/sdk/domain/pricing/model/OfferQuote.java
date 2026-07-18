/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferQuoteDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * One current fee quote for a single offer, as returned by
 * {@code pricing().quotes(offerIds)}: the fee the seller currently pays for a
 * named quote {@code type}, whether it is active, and when it next applies.
 *
 * @param offerId the offer the quote is for
 * @param type the quote type identifier
 * @param name the human-readable quote name
 * @param enabled whether the quote is currently active
 * @param feeAmount the fee amount, or {@code null} when the quote carries none
 * @param nextDate when the quote next applies, or {@code null} when not scheduled
 *
 * @since 0.3.0
 */
public record OfferQuote(
        @Nullable String offerId,
        @Nullable String type,
        @Nullable String name,
        boolean enabled,
        @Nullable Money feeAmount,
        @Nullable Instant nextDate) {

    /**
     * Map the generated response DTO to the public record.
     *
     * @param raw the generated offer-quote DTO
     * @return the mapped record
     */
    public static OfferQuote from(OfferQuoteDtoRaw raw) {
        return new OfferQuote(
                raw.getOffer() == null ? null : raw.getOffer().getId(),
                raw.getType(),
                raw.getName(),
                Boolean.TRUE.equals(raw.getEnabled()),
                raw.getFee() == null
                        ? null
                        : Money.of(raw.getFee().getAmount(), raw.getFee().getCurrency()),
                raw.getNextDate() == null ? null : raw.getNextDate().toInstant());
    }
}
