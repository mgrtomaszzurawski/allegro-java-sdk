/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.campaigns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ExcludeOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.OfferScope;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.OfferSubstatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ParticipationUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.ParticipationStatus;
import org.junit.jupiter.api.Test;

/**
 * Round-trip unit tests for the Allegro Prices inputs: participation update,
 * offer-status query, and the submit / exclude command requests — every builder
 * method, {@code toBuilder} preservation, and the required-field / offer-count
 * bounds (TESTING.md §1). No wire.
 */
class AllegroPricesBuildersTest {

    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String MARKETPLACE_CZ = "allegro-cz";
    private static final String OFFER_ID = "12345678";
    private static final String MAX_CONTRIBUTION = "5";
    private static final int MAX_OFFERS = 1000;
    private static final int OVER_MAX_OFFERS = 1001;

    @Test
    void participationUpdate_whenAllowAndDeny_recordsBothMarketplaces() {
        // when
        ParticipationUpdate update = ParticipationUpdate.builder()
                .allow(MARKETPLACE_PL)
                .deny(MARKETPLACE_CZ)
                .build();

        // then
        assertEquals(2, update.marketplaces().size());
        assertEquals(ParticipationStatus.ALLOWED, update.marketplaces().get(0).status());
        assertEquals(ParticipationStatus.DENIED, update.marketplaces().get(1).status());
    }

    @Test
    void participationUpdate_whenEmpty_throwsIllegalState() {
        // given
        ParticipationUpdate.Builder builder = ParticipationUpdate.builder();

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void participationUpdate_toBuilder_preservesEntries() {
        // given
        ParticipationUpdate original = ParticipationUpdate.builder()
                .allow(MARKETPLACE_PL)
                .deny(MARKETPLACE_CZ)
                .build();

        // when
        ParticipationUpdate copy = original.toBuilder().build();

        // then
        assertEquals(original.marketplaces(), copy.marketplaces());
    }

    @Test
    void offerQuery_whenAllFiltersSet_retainsEveryValue() {
        // when
        AllegroPricesOfferQuery query = AllegroPricesOfferQuery.builder(MARKETPLACE_PL)
                .scope(OfferScope.DISCOUNTED)
                .substatus(OfferSubstatus.DISCOUNT_OPPORTUNITY)
                .addOfferId(OFFER_ID)
                .build();

        // then
        assertEquals(MARKETPLACE_PL, query.marketplaceId());
        assertEquals(OfferScope.DISCOUNTED, query.scope());
        assertEquals(OfferSubstatus.DISCOUNT_OPPORTUNITY, query.substatus());
        assertEquals(1, query.offerIds().size());
    }

    @Test
    void offerQuery_whenMarketplaceBlank_throwsIllegalArgument() {
        // then
        assertThrows(IllegalArgumentException.class, () -> AllegroPricesOfferQuery.builder("  "));
    }

    @Test
    void offerQuery_toBuilder_preservesFields() {
        // given
        AllegroPricesOfferQuery original = AllegroPricesOfferQuery.builder(MARKETPLACE_PL)
                .scope(OfferScope.EXCLUDED)
                .addOfferId(OFFER_ID)
                .build();

        // when
        AllegroPricesOfferQuery copy = original.toBuilder().build();

        // then
        assertEquals(original.marketplaceId(), copy.marketplaceId());
        assertEquals(original.scope(), copy.scope());
        assertEquals(original.offerIds(), copy.offerIds());
        assertNull(copy.substatus());
    }

    @Test
    void submitRequest_whenOffersAdded_retainsContributionAndMarketplace() {
        // when
        SubmitOffersRequest request = SubmitOffersRequest.builder()
                .addOffer(OFFER_ID, MARKETPLACE_PL)
                .addOffer(OFFER_ID, MARKETPLACE_CZ, MAX_CONTRIBUTION)
                .build();

        // then
        assertEquals(2, request.offers().size());
        assertNull(request.offers().get(0).maxContributionPercentage());
        assertEquals(MAX_CONTRIBUTION, request.offers().get(1).maxContributionPercentage());
    }

    @Test
    void submitRequest_whenEmpty_throwsIllegalState() {
        // given
        SubmitOffersRequest.Builder builder = SubmitOffersRequest.builder();

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void submitRequest_whenOverMaxOffers_throwsIllegalState() {
        // given
        SubmitOffersRequest.Builder builder = SubmitOffersRequest.builder();
        for (int index = 0; index < OVER_MAX_OFFERS; index++) {
            builder.addOffer(OFFER_ID, MARKETPLACE_PL);
        }

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void submitRequest_atMaxOffers_builds() {
        // given
        SubmitOffersRequest.Builder builder = SubmitOffersRequest.builder();
        for (int index = 0; index < MAX_OFFERS; index++) {
            builder.addOffer(OFFER_ID, MARKETPLACE_PL);
        }

        // when
        SubmitOffersRequest request = builder.build();

        // then
        assertEquals(MAX_OFFERS, request.offers().size());
    }

    @Test
    void submitRequest_toBuilder_preservesOffers() {
        // given
        SubmitOffersRequest original = SubmitOffersRequest.builder()
                .addOffer(OFFER_ID, MARKETPLACE_PL, MAX_CONTRIBUTION)
                .build();

        // when
        SubmitOffersRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.offers(), copy.offers());
    }

    @Test
    void excludeRequest_whenOffersAdded_retainsEntries() {
        // when
        ExcludeOffersRequest request = ExcludeOffersRequest.builder()
                .addOffer(OFFER_ID, MARKETPLACE_PL)
                .build();

        // then
        assertEquals(1, request.offers().size());
        assertEquals(OFFER_ID, request.offers().get(0).offerId());
    }

    @Test
    void excludeRequest_whenEmpty_throwsIllegalState() {
        // given
        ExcludeOffersRequest.Builder builder = ExcludeOffersRequest.builder();

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void excludeRequest_toBuilder_preservesOffers() {
        // given
        ExcludeOffersRequest original = ExcludeOffersRequest.builder()
                .addOffer(OFFER_ID, MARKETPLACE_PL)
                .build();

        // when
        ExcludeOffersRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.offers(), copy.offers());
    }
}
