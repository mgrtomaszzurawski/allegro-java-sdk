/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;
import java.util.List;

/**
 * Pricing tools for the authenticated seller — reached via
 * {@code AllegroClient.pricing()}.
 *
 * @since 0.2.0
 */
public interface Pricing {

    /**
     * Automatic pricing rules — follow-the-market price strategies the seller
     * defines once and assigns to offers.
     *
     * @return the automatic-pricing sub-facade
     */
    PricingAutomation automation();

    /**
     * The seller's current fee quotes for the given offers — the recurring fees
     * (such as promoted-listing quotes) each offer is currently subject to.
     *
     * @param offerIds the offers to quote (at least one); sent as repeated
     *     {@code offer.id} filters
     * @return one {@link OfferQuote} per matching offer/quote pairing
     */
    List<OfferQuote> quotes(List<String> offerIds);

    /**
     * List the deposit types the seller can attach to offers (e.g. returnable
     * packaging or bottle deposits).
     *
     * @return the available deposit types
     */
    List<DepositType> depositTypes();
}
