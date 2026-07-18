/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.campaigns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.EligibleOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmittedOffersFilter;
import org.junit.jupiter.api.Test;

/**
 * Round-trip unit tests for the AlleDiscount inputs: the submit-offer request and
 * the eligible / submitted offer filters — every builder method, {@code toBuilder}
 * preservation, and required-field fail-fast (TESTING.md §1). No wire.
 */
class AlleDiscountBuildersTest {

    private static final String CAMPAIGN_ID = "winter-sale";
    private static final String OFFER_ID = "12345678";
    private static final String PARTICIPATION_ID = "part-1";
    private static final Money PROPOSED_PRICE = Money.of("24.99", "PLN");

    @Test
    void submitRequest_whenAllRequiredFieldsSet_retainsEveryValue() {
        // when
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .proposedPrice(PROPOSED_PRICE)
                .build();

        // then
        assertEquals(CAMPAIGN_ID, request.campaignId());
        assertEquals(OFFER_ID, request.offerId());
        assertEquals(PROPOSED_PRICE, request.proposedPrice());
    }

    @Test
    void submitRequest_toBuilder_preservesAllFields() {
        // given
        SubmitOfferRequest original = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(OFFER_ID).proposedPrice(PROPOSED_PRICE).build();

        // when
        SubmitOfferRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.campaignId(), copy.campaignId());
        assertEquals(original.offerId(), copy.offerId());
        assertEquals(original.proposedPrice(), copy.proposedPrice());
    }

    @Test
    void submitRequest_whenCampaignMissing_throwsIllegalState() {
        // given
        SubmitOfferRequest.Builder builder = SubmitOfferRequest.builder()
                .offerId(OFFER_ID).proposedPrice(PROPOSED_PRICE);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void submitRequest_whenOfferMissing_throwsIllegalState() {
        // given
        SubmitOfferRequest.Builder builder = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).proposedPrice(PROPOSED_PRICE);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void submitRequest_whenPriceMissing_throwsIllegalState() {
        // given
        SubmitOfferRequest.Builder builder = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(OFFER_ID);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void eligibleFilter_whenAllFieldsSet_retainsEveryValue() {
        // when
        EligibleOffersFilter filter = EligibleOffersFilter.builder(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .meetsConditions(true)
                .build();

        // then
        assertEquals(CAMPAIGN_ID, filter.campaignId());
        assertEquals(OFFER_ID, filter.offerId());
        assertTrue(filter.meetsConditions());
    }

    @Test
    void eligibleFilter_whenCampaignBlank_throwsIllegalArgument() {
        // then
        assertThrows(IllegalArgumentException.class, () -> EligibleOffersFilter.builder("  "));
    }

    @Test
    void eligibleFilter_toBuilder_preservesFields() {
        // given
        EligibleOffersFilter original = EligibleOffersFilter.builder(CAMPAIGN_ID)
                .offerId(OFFER_ID).meetsConditions(false).build();

        // when
        EligibleOffersFilter copy = original.toBuilder().build();

        // then
        assertEquals(CAMPAIGN_ID, copy.campaignId());
        assertEquals(OFFER_ID, copy.offerId());
        assertEquals(Boolean.FALSE, copy.meetsConditions());
    }

    @Test
    void submittedFilter_whenAllFieldsSet_retainsEveryValue() {
        // when
        SubmittedOffersFilter filter = SubmittedOffersFilter.builder(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .participationId(PARTICIPATION_ID)
                .build();

        // then
        assertEquals(CAMPAIGN_ID, filter.campaignId());
        assertEquals(OFFER_ID, filter.offerId());
        assertEquals(PARTICIPATION_ID, filter.participationId());
    }

    @Test
    void submittedFilter_whenCampaignBlank_throwsIllegalArgument() {
        // then
        assertThrows(IllegalArgumentException.class, () -> SubmittedOffersFilter.builder("  "));
    }

    @Test
    void submittedFilter_toBuilder_preservesFields() {
        // given
        SubmittedOffersFilter original = SubmittedOffersFilter.builder(CAMPAIGN_ID)
                .offerId(OFFER_ID).participationId(PARTICIPATION_ID).build();

        // when
        SubmittedOffersFilter copy = original.toBuilder().build();

        // then
        assertEquals(CAMPAIGN_ID, copy.campaignId());
        assertEquals(OFFER_ID, copy.offerId());
        assertEquals(PARTICIPATION_ID, copy.participationId());
    }

    @Test
    void submittedFilter_whenOnlyCampaignSet_leavesOptionalsNull() {
        // when
        SubmittedOffersFilter filter = SubmittedOffersFilter.builder(CAMPAIGN_ID).build();

        // then
        assertNull(filter.offerId());
        assertNull(filter.participationId());
    }
}
