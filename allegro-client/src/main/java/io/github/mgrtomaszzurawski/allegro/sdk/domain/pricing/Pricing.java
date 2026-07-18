/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
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
     * Rebate promotions — large-order discounts, wholesale price lists and
     * multipack rewards the seller targets at chosen offers.
     *
     * @return the promotions sub-facade
     * @since 0.4.0
     */
    Promotions promotions();

    /**
     * Marketplace turnover discounts — reward a buyer's cumulated turnover with
     * a percentage discount.
     *
     * @return the turnover-discount sub-facade
     * @since 0.3.0
     */
    TurnoverDiscounts turnoverDiscounts();

    /**
     * Preview the fees a draft offer would incur (sale commission and any
     * recurring quotes) before publishing it.
     *
     * @param request the draft-offer details, built with
     *     {@link OfferFeePreviewRequest#builder()}
     * @return the previewed commissions and quotes
     * @since 0.3.0
     */
    FeePreview feePreview(OfferFeePreviewRequest request);

    /**
     * The seller's current fee quotes for the given offers — the recurring fees
     * (such as promoted-listing quotes) each offer is currently subject to.
     *
     * @param offerIds the offers to quote (at least one); sent as repeated
     *     {@code offer.id} filters
     * @return one {@link OfferQuote} per matching offer/quote pairing
     * @throws IllegalArgumentException if {@code offerIds} is null or empty
     * @since 0.3.0
     */
    List<OfferQuote> quotes(List<String> offerIds);

    /**
     * List the deposit types the seller can attach to offers (e.g. returnable
     * packaging or bottle deposits).
     *
     * @return the available deposit types
     * @since 0.3.0
     */
    List<DepositType> depositTypes();
}
