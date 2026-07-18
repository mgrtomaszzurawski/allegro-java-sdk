/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import io.github.mgrtomaszzurawski.allegro.client.model.AccountParticipationMarketplaceRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ActualPriceReductionDtoFinalPriceForTheBuyerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ActualPriceReductionDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPricesAccountParticipationRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeclaredPriceReductionDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MoneyDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoActualPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoDeclaredPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoDiscountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoRecommendedPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusQueryRequestDtoMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusQueryRequestDtoOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusQueryRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RecommendedPriceReductionDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyExcludeOffersCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyOfferToExcludeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyOfferToSubmitMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyOfferToSubmitRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidyOfferToSubmitSellerDiscountDeclarationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubsidySubmitOffersCommandRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ExcludeOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.OfferScope;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.OfferSubstatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ParticipationUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesOfferStatus;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Package-private mapper between the Allegro Prices domain types and the wire form.
 *
 * <p>Both writes and reads map through the generated DTOs. The offer-status
 * response's per-stage price-reduction fields are a generated
 * {@code oneOf[Object, …Dto]}; the SDK's strict {@code oneOf} mapper (core C5)
 * resolves each wrapper to its concrete {@code …PriceReductionDtoRaw} — or leaves an
 * empty {@code Object} when the stage does not apply — so they read through typed
 * getters like every other field (the BACKLOG Phase 1.1 raw-JSON workaround is
 * retired now that the strict resolver no longer over-matches the {@code Object}
 * branch).
 */
final class AllegroPricesMapper {

    private static final char MARKETPLACE_ID_SEPARATOR = '-';
    private static final char MARKETPLACE_ENUM_SEPARATOR = '_';

    private AllegroPricesMapper() {
    }

    static AllegroPricesAccountParticipationRequestRaw toRaw(ParticipationUpdate update) {
        AllegroPricesAccountParticipationRequestRaw raw =
                new AllegroPricesAccountParticipationRequestRaw();
        update.marketplaces().forEach(entry -> raw.addMarketplacesItem(
                new AccountParticipationMarketplaceRequestRaw()
                        .id(entry.marketplaceId())
                        .status(entry.status().name())));
        return raw;
    }

    static OfferStatusQueryRequestDtoRaw toRaw(AllegroPricesOfferQuery query, int offset, int limit) {
        OfferStatusQueryRequestDtoOfferRaw offer = new OfferStatusQueryRequestDtoOfferRaw();
        query.offerIds().forEach(offer::addIdsItem);
        OfferScope scope = query.scope();
        if (scope != null) {
            offer.scope(OfferStatusQueryRequestDtoOfferRaw.ScopeEnum.valueOf(scope.name()));
        }
        OfferSubstatus substatus = query.substatus();
        if (substatus != null) {
            offer.substatus(OfferStatusQueryRequestDtoOfferRaw.SubstatusEnum.valueOf(substatus.name()));
        }
        return new OfferStatusQueryRequestDtoRaw()
                .offer(offer)
                .marketplace(new OfferStatusQueryRequestDtoMarketplaceRaw().id(marketplaceId(query.marketplaceId())))
                .offset(offset)
                .limit(limit);
    }

    static SubsidySubmitOffersCommandRaw toRaw(SubmitOffersRequest request) {
        SubsidySubmitOffersCommandRaw raw = new SubsidySubmitOffersCommandRaw();
        request.offers().forEach(offer -> raw.addOffersItem(submitItem(offer)));
        return raw;
    }

    static SubsidyExcludeOffersCommandRaw toRaw(ExcludeOffersRequest request) {
        SubsidyExcludeOffersCommandRaw raw = new SubsidyExcludeOffersCommandRaw();
        request.offers().forEach(offer -> raw.addOffersItem(new SubsidyOfferToExcludeRaw()
                .id(offer.offerId())
                .marketplace(new SubsidyOfferToSubmitMarketplaceRaw().id(offer.marketplaceId()))));
        return raw;
    }

    static AllegroPricesOfferStatus offerStatusFrom(OfferStatusItemDtoRaw item) {
        ActualPriceReductionDtoRaw actual = actualReduction(item.getActualPriceReduction());
        return new AllegroPricesOfferStatus(
                item.getId(),
                item.getName(),
                item.getMarketplace().getId(),
                money(item.getBasePrice()),
                discountOpportunity(item.getDiscount()),
                recommendedPercentage(item.getRecommendedPriceReduction()),
                declaredPercentage(item.getDeclaredPriceReduction()),
                actual == null ? null : actual.getSellerMaxDeclaredPercentage(),
                actual == null ? null : money(actual.getFinalPriceForTheBuyer()),
                item.getDiscountedAt(),
                item.getExcludedAt());
    }

    private static SubsidyOfferToSubmitRaw submitItem(SubmitOffersRequest.Offer offer) {
        SubsidyOfferToSubmitRaw raw = new SubsidyOfferToSubmitRaw()
                .id(offer.offerId())
                .marketplace(new SubsidyOfferToSubmitMarketplaceRaw().id(offer.marketplaceId()));
        String maxContribution = offer.maxContributionPercentage();
        if (maxContribution != null) {
            raw.sellerDiscountDeclaration(new SubsidyOfferToSubmitSellerDiscountDeclarationRaw()
                    .maxContributionPercentage(maxContribution));
        }
        return raw;
    }

    private static OfferStatusQueryRequestDtoMarketplaceRaw.IdEnum marketplaceId(String marketplaceId) {
        String enumName = marketplaceId.toUpperCase(Locale.ROOT)
                .replace(MARKETPLACE_ID_SEPARATOR, MARKETPLACE_ENUM_SEPARATOR);
        return OfferStatusQueryRequestDtoMarketplaceRaw.IdEnum.valueOf(enumName);
    }

    private static boolean discountOpportunity(@Nullable OfferStatusItemDtoDiscountRaw discount) {
        return discount != null && Boolean.TRUE.equals(discount.getOpportunity());
    }

    private static @Nullable String recommendedPercentage(
            @Nullable OfferStatusItemDtoRecommendedPriceReductionRaw reduction) {
        if (reduction != null
                && reduction.getActualInstance() instanceof RecommendedPriceReductionDtoRaw recommended) {
            return recommended.getSellerMaxDeclaredPercentage();
        }
        return null;
    }

    private static @Nullable String declaredPercentage(
            @Nullable OfferStatusItemDtoDeclaredPriceReductionRaw reduction) {
        if (reduction != null
                && reduction.getActualInstance() instanceof DeclaredPriceReductionDtoRaw declared) {
            return declared.getSellerMaxDeclaredPercentage();
        }
        return null;
    }

    private static @Nullable ActualPriceReductionDtoRaw actualReduction(
            @Nullable OfferStatusItemDtoActualPriceReductionRaw reduction) {
        if (reduction != null
                && reduction.getActualInstance() instanceof ActualPriceReductionDtoRaw actual) {
            return actual;
        }
        return null;
    }

    private static @Nullable Money money(@Nullable MoneyDtoRaw price) {
        return price == null ? null : money(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money money(@Nullable ActualPriceReductionDtoFinalPriceForTheBuyerRaw price) {
        return price == null ? null : money(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money money(@Nullable String amount, @Nullable String currency) {
        return amount == null || currency == null ? null : Money.of(amount, currency);
    }
}
