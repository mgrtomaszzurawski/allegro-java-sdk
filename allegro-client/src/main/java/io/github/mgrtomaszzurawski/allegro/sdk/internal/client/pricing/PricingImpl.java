/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DepositTypeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FeePreviewResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferQuotesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PricingOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicOfferPreviewRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeWithNetPriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Pricing;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.TurnoverDiscounts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;

/**
 * Root implementation behind the {@link Pricing} facade. Holds the sub-facade
 * implementations and hands them out, and serves the top-level reads (fee
 * quotes, deposit types) directly; each shares the same {@link HttpRuntime}.
 *
 * @since 0.2.0
 */
public final class PricingImpl implements Pricing {

    private static final String OP_FEE_PREVIEW = "preview offer fees";
    private static final String OP_QUOTES = "get offer fee quotes";
    private static final String OP_DEPOSIT_TYPES = "list deposit types";
    private static final String QUERY_OFFER_ID = "offer.id";
    private static final String ERR_NO_OFFER_IDS = "at least one offer id is required";

    private final HttpSupport http;
    private final PricingAutomation automation;
    private final TurnoverDiscounts turnoverDiscounts;

    public PricingImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.automation = new PricingAutomationImpl(runtime);
        this.turnoverDiscounts = new TurnoverDiscountsImpl(runtime);
    }

    @Override
    public PricingAutomation automation() {
        return automation;
    }

    @Override
    public TurnoverDiscounts turnoverDiscounts() {
        return turnoverDiscounts;
    }

    @Override
    public FeePreview feePreview(OfferFeePreviewRequest request) {
        FeePreviewResponseRaw response = http.request(OP_FEE_PREVIEW)
                .post(ApiPaths.OFFER_FEE_PREVIEW)
                .jsonBody(feePreviewToRaw(request))
                .fetch(FeePreviewResponseRaw.class);
        return FeePreview.from(response);
    }

    @Override
    public List<OfferQuote> quotes(List<String> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_NO_OFFER_IDS);
        }
        OfferQuotesDtoRaw response = http.request(OP_QUOTES)
                .get(ApiPaths.OFFER_QUOTES)
                .query(Query.create().addAll(QUERY_OFFER_ID, offerIds))
                .fetch(OfferQuotesDtoRaw.class);
        return response.getQuotes() == null
                ? List.of()
                : response.getQuotes().stream().map(OfferQuote::from).toList();
    }

    @Override
    public List<DepositType> depositTypes() {
        DepositTypeResponseRaw response = http.request(OP_DEPOSIT_TYPES)
                .get(ApiPaths.DEPOSIT_TYPES)
                .fetch(DepositTypeResponseRaw.class);
        return response.getDeposits() == null
                ? List.of()
                : response.getDeposits().stream().map(DepositType::from).toList();
    }

    /**
     * Build the Buy-Now offer-preview request body from the focused domain
     * request: only the category and price the fee calculation needs.
     */
    private static PublicOfferPreviewRequestRaw feePreviewToRaw(OfferFeePreviewRequest request) {
        PricingOfferRaw offer = new PricingOfferRaw()
                .category(new CategoryRaw().id(request.categoryId()))
                .sellingMode(new SellingModeWithNetPriceRaw()
                        .format(SellingModeFormatRaw.BUY_NOW)
                        .price(new BuyNowPriceRaw()
                                .amount(request.price().amount())
                                .currency(request.price().currency())));
        if (request.offerId() != null) {
            offer.id(request.offerId());
        }
        return new PublicOfferPreviewRequestRaw().offer(offer);
    }
}
