/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.client.model.AccountParticipationMarketplaceRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPricesAccountParticipationRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusQueryRequestDtoMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusQueryRequestDtoOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusQueryRequestDtoRaw;
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
import java.time.OffsetDateTime;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Package-private mapper between the Allegro Prices domain types and the wire form.
 *
 * <p><strong>Writes</strong> use the generated request DTOs. The <strong>offer
 * status read</strong> is mapped from a {@link JsonNode}: the response's per-stage
 * price-reduction fields are a generated {@code oneOf[Object, …Dto]}, and under the
 * SDK's forward-compatible mapper the {@code Object} branch matches every payload,
 * so the generated deserializer fails with "2 classes match". Reading from the raw
 * JSON side-steps that generator flaw (BACKLOG Phase 1.1; same approach as
 * {@code PricingMapper}).
 */
final class AllegroPricesMapper {

    private static final char MARKETPLACE_ID_SEPARATOR = '-';
    private static final char MARKETPLACE_ENUM_SEPARATOR = '_';

    private static final String FIELD_OFFERS = "offers";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MARKETPLACE = "marketplace";
    private static final String FIELD_BASE_PRICE = "basePrice";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_DISCOUNT = "discount";
    private static final String FIELD_OPPORTUNITY = "opportunity";
    private static final String FIELD_RECOMMENDED = "recommendedPriceReduction";
    private static final String FIELD_DECLARED = "declaredPriceReduction";
    private static final String FIELD_ACTUAL = "actualPriceReduction";
    private static final String FIELD_SELLER_MAX_PCT = "sellerMaxDeclaredPercentage";
    private static final String FIELD_FINAL_PRICE = "finalPriceForTheBuyer";
    private static final String FIELD_DISCOUNTED_AT = "discountedAt";
    private static final String FIELD_EXCLUDED_AT = "excludedAt";

    private AllegroPricesMapper() {
    }

    /** The {@code offers} array from an offer-status query response, or {@code null}. */
    static @Nullable JsonNode offersArray(JsonNode response) {
        return response.get(FIELD_OFFERS);
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

    static AllegroPricesOfferStatus offerStatusFrom(JsonNode item) {
        return new AllegroPricesOfferStatus(
                item.get(FIELD_ID).asText(),
                item.get(FIELD_NAME).asText(),
                item.get(FIELD_MARKETPLACE).get(FIELD_ID).asText(),
                money(item.get(FIELD_BASE_PRICE)),
                discountOpportunity(item.get(FIELD_DISCOUNT)),
                percentage(item.get(FIELD_RECOMMENDED)),
                percentage(item.get(FIELD_DECLARED)),
                percentage(item.get(FIELD_ACTUAL)),
                money(finalPriceNode(item.get(FIELD_ACTUAL))),
                dateTime(item.get(FIELD_DISCOUNTED_AT)),
                dateTime(item.get(FIELD_EXCLUDED_AT)));
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

    private static boolean discountOpportunity(@Nullable JsonNode discount) {
        return discount != null && discount.hasNonNull(FIELD_OPPORTUNITY)
                && discount.get(FIELD_OPPORTUNITY).asBoolean();
    }

    private static @Nullable String percentage(@Nullable JsonNode reduction) {
        if (reduction == null || !reduction.hasNonNull(FIELD_SELLER_MAX_PCT)) {
            return null;
        }
        return reduction.get(FIELD_SELLER_MAX_PCT).asText();
    }

    private static @Nullable JsonNode finalPriceNode(@Nullable JsonNode actualReduction) {
        return actualReduction == null ? null : actualReduction.get(FIELD_FINAL_PRICE);
    }

    private static @Nullable Money money(@Nullable JsonNode price) {
        if (price == null || !price.hasNonNull(FIELD_AMOUNT) || !price.hasNonNull(FIELD_CURRENCY)) {
            return null;
        }
        return Money.of(price.get(FIELD_AMOUNT).asText(), price.get(FIELD_CURRENCY).asText());
    }

    private static @Nullable OffsetDateTime dateTime(@Nullable JsonNode node) {
        return node == null || node.isNull() ? null : OffsetDateTime.parse(node.asText());
    }
}
