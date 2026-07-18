/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyExcludeOfferItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOfferItemRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The outcome for a single offer within an Allegro Prices {@code submitOffers} or
 * {@code excludeOffers} command.
 *
 * @param offerId       the offer
 * @param marketplaceId the marketplace the offer was processed on
 * @param status        per-offer outcome
 * @param errors        failure messages when {@code status} is
 *                      {@link SubsidyOfferStatus#FAILED}; never {@code null}, possibly empty
 * @param completedAt   when the offer reached its terminal state, or {@code null}
 *
 * @since 0.2.0
 */
public record SubsidyOfferResult(
        String offerId,
        String marketplaceId,
        SubsidyOfferStatus status,
        List<String> errors,
        @Nullable OffsetDateTime completedAt) {

    public SubsidyOfferResult {
        errors = List.copyOf(errors);
    }

    /** Map a submit-command per-offer item to the public record. */
    static SubsidyOfferResult from(SubsidySubmitOfferItemRaw raw) {
        return new SubsidyOfferResult(
                raw.getId(),
                raw.getMarketplace().getId(),
                SubsidyOfferStatus.from(raw.getStatus().name()),
                errorMessages(raw.getErrors()),
                raw.getCompletedAt());
    }

    /** Map an exclusion-command per-offer item to the public record. */
    static SubsidyOfferResult from(SubsidyExcludeOfferItemRaw raw) {
        return new SubsidyOfferResult(
                raw.getId(),
                raw.getMarketplace().getId(),
                SubsidyOfferStatus.from(raw.getStatus().name()),
                errorMessages(raw.getErrors()),
                raw.getCompletedAt());
    }

    private static List<String> errorMessages(@Nullable List<OfferErrorRaw> errors) {
        return errors == null ? List.of() : errors.stream().map(OfferErrorRaw::getMessage).toList();
    }
}
