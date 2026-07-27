/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyExcludeOfferItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOfferItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOfferItemSellerDiscountDeclarationRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The outcome for a single offer within an Allegro Prices {@code submitOffers} or
 * {@code excludeOffers} command.
 *
 * <p><strong>Alpha:</strong> {@link #maxContributionPercentage()} is mapped from the vendored
 * OpenAPI spec but has not yet been confirmed against the live Allegro sandbox — a subsidy submit
 * requires an offer with a discount opportunity, which the current sandbox account cannot arrange.
 * Verify this field manually before relying on it, or treat it as alpha. The other components are
 * exercised on the live sandbox.
 *
 * @param offerId                   the offer
 * @param marketplaceId             the marketplace the offer was processed on
 * @param status                    per-offer outcome
 * @param errors                    failure messages when {@code status} is
 *                                  {@link SubsidyOfferStatus#FAILED}; never {@code null},
 *                                  possibly empty
 * @param completedAt               when the offer reached its terminal state, or {@code null}
 * @param maxContributionPercentage the seller's declared maximum subsidy contribution for the
 *                                  offer, as Allegro recorded it (echoed back from the submit
 *                                  declaration); {@code null} on an exclusion, or when the offer
 *                                  carried no declaration
 *
 * @since 0.2.0
 */
public record SubsidyOfferResult(
        String offerId,
        String marketplaceId,
        SubsidyOfferStatus status,
        List<String> errors,
        @Nullable OffsetDateTime completedAt,
        @Nullable String maxContributionPercentage) {

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
                raw.getCompletedAt(),
                declaredMaxContribution(raw.getSellerDiscountDeclaration()));
    }

    /** Map an exclusion-command per-offer item to the public record. */
    static SubsidyOfferResult from(SubsidyExcludeOfferItemRaw raw) {
        return new SubsidyOfferResult(
                raw.getId(),
                raw.getMarketplace().getId(),
                SubsidyOfferStatus.from(raw.getStatus().name()),
                errorMessages(raw.getErrors()),
                raw.getCompletedAt(),
                null);
    }

    private static @Nullable String declaredMaxContribution(
            @Nullable SubsidySubmitOfferItemSellerDiscountDeclarationRaw declaration) {
        return declaration == null ? null : declaration.getMaxContributionPercentage();
    }

    private static List<String> errorMessages(@Nullable List<OfferErrorRaw> errors) {
        return errors == null ? List.of() : errors.stream().map(OfferErrorRaw::getMessage).toList();
    }
}
