/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyExcludeOffersCommandPreviewRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOffersCommandPreviewRaw;
import java.util.List;

/**
 * The terminal result of an Allegro Prices {@code submitOffers} or
 * {@code excludeOffers} command: the command id and the per-offer outcomes. The
 * SDK polls the command to completion before returning this, so every
 * {@link SubsidyOfferResult#status()} is {@link SubsidyOfferStatus#SUCCESS} or
 * {@link SubsidyOfferStatus#FAILED}.
 *
 * @param commandId the command id (useful for support and idempotent re-reads)
 * @param offers    per-offer outcomes; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record SubsidyCommandReport(String commandId, List<SubsidyOfferResult> offers) {

    public SubsidyCommandReport {
        offers = List.copyOf(offers);
    }

    /** Map a submit-command preview (poll response) to the public record. */
    public static SubsidyCommandReport from(SubsidySubmitOffersCommandPreviewRaw raw) {
        return new SubsidyCommandReport(
                raw.getCommandId(),
                raw.getOffers().stream().map(SubsidyOfferResult::from).toList());
    }

    /** Map an exclusion-command preview (poll response) to the public record. */
    public static SubsidyCommandReport from(SubsidyExcludeOffersCommandPreviewRaw raw) {
        return new SubsidyCommandReport(
                raw.getCommandId(),
                raw.getOffers().stream().map(SubsidyOfferResult::from).toList());
    }
}
