/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedExtraPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedsPackagesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DepositTypeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FeePreviewResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NetPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferQuotesDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRangeValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PricingOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PricingPublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromotionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicOfferPreviewRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeWithNetPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Pricing;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Promotions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.TurnoverDiscounts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ClassifiedsExtraPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ClassifiedsPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreviewSellingMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.ParameterRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import org.jspecify.annotations.Nullable;

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
    private final Promotions promotions;
    private final TurnoverDiscounts turnoverDiscounts;

    public PricingImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.automation = new PricingAutomationImpl(runtime);
        this.promotions = new PromotionsImpl(runtime);
        this.turnoverDiscounts = new TurnoverDiscountsImpl(runtime);
    }

    @Override
    public PricingAutomation automation() {
        return automation;
    }

    @Override
    public Promotions promotions() {
        return promotions;
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
     * Build the offer-preview request body from the domain request, carrying
     * every fee-affecting input the caller supplied. Optional blocks are added
     * only when set, so a bare category-and-price request stays minimal.
     */
    private static PublicOfferPreviewRequestRaw feePreviewToRaw(OfferFeePreviewRequest request) {
        PricingOfferRaw offer = new PricingOfferRaw()
                .category(new CategoryRaw().id(request.categoryId()))
                .sellingMode(sellingModeToRaw(request.sellingMode()));
        if (request.offerId() != null) {
            offer.id(request.offerId());
        }
        if (request.fundraisingCampaignId() != null) {
            offer.fundraisingCampaign(new JustIdRaw().id(request.fundraisingCampaignId()));
        }
        if (request.publicationDuration() != null) {
            offer.publication(new PricingPublicationRaw().duration(request.publicationDuration()));
        }
        PromotionRaw promotion = promotionToRaw(request.promotionOptions());
        if (promotion != null) {
            offer.promotion(promotion);
        }
        for (OfferParameter parameter : request.parameters()) {
            offer.addParametersItem(parameterToRaw(parameter));
        }
        PublicOfferPreviewRequestRaw body = new PublicOfferPreviewRequestRaw().offer(offer);
        if (request.marketplaceId() != null) {
            body.marketplaceId(request.marketplaceId());
        }
        if (request.classifiedsPackages() != null) {
            body.classifiedsPackages(classifiedsToRaw(request.classifiedsPackages()));
        }
        return body;
    }

    private static SellingModeWithNetPriceRaw sellingModeToRaw(FeePreviewSellingMode mode) {
        SellingModeWithNetPriceRaw raw = new SellingModeWithNetPriceRaw();
        if (mode instanceof FeePreviewSellingMode.BuyNow buyNow) {
            raw.format(SellingModeFormatRaw.BUY_NOW)
                    .price(new BuyNowPriceRaw()
                            .amount(buyNow.price().amount())
                            .currency(buyNow.price().currency()));
            if (buyNow.netPrice() != null) {
                raw.netPrice(new NetPriceRaw()
                        .amount(buyNow.netPrice().amount())
                        .currency(buyNow.netPrice().currency()));
            }
        } else if (mode instanceof FeePreviewSellingMode.Auction auction) {
            raw.format(SellingModeFormatRaw.AUCTION)
                    .startingPrice(new StartingPriceRaw()
                            .amount(auction.startingPrice().amount())
                            .currency(auction.startingPrice().currency()));
            if (auction.minimalPrice() != null) {
                raw.minimalPrice(new MinimalPriceRaw()
                        .amount(auction.minimalPrice().amount())
                        .currency(auction.minimalPrice().currency()));
            }
        }
        return raw;
    }

    private static @Nullable PromotionRaw promotionToRaw(PromotionOptions options) {
        if (!options.any()) {
            return null;
        }
        PromotionRaw raw = new PromotionRaw();
        if (options.emphasized1d()) {
            raw.emphasized1d(true);
        }
        if (options.emphasized10d()) {
            raw.emphasized10d(true);
        }
        if (options.departmentPage()) {
            raw.departmentPage(true);
        }
        return raw;
    }

    private static ParameterRaw parameterToRaw(OfferParameter parameter) {
        ParameterRaw raw = new ParameterRaw().id(parameter.id());
        if (!parameter.values().isEmpty()) {
            raw.values(parameter.values());
        }
        if (!parameter.valuesIds().isEmpty()) {
            raw.valuesIds(parameter.valuesIds());
        }
        ParameterRange range = parameter.rangeValue();
        if (range != null) {
            raw.rangeValue(new ParameterRangeValueRaw().from(range.lowerBound()).to(range.upperBound()));
        }
        return raw;
    }

    private static ClassifiedsPackagesRaw classifiedsToRaw(ClassifiedsPackages packages) {
        ClassifiedsPackagesRaw raw = new ClassifiedsPackagesRaw();
        if (packages.basePackageId() != null) {
            raw.basePackage(new ClassifiedPackageRaw().id(packages.basePackageId()));
        }
        for (ClassifiedsExtraPackage extra : packages.extraPackages()) {
            raw.addExtraPackagesItem(new ClassifiedExtraPackageRaw()
                    .id(extra.id())
                    .republish(extra.republish()));
        }
        return raw;
    }
}
