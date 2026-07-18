/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.campaigns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgePatch;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Round-trip unit tests for the badge request, filter and patch inputs: every
 * fluent builder method, {@code toBuilder} preservation, required-field
 * fail-fast, and {@link BadgePatch} value semantics (TESTING.md §1). No wire.
 */
class CampaignsBuildersTest {

    private static final String CAMPAIGN_ID = "BARGAIN";
    private static final String OFFER_ID = "12345678";
    private static final String MARKETPLACE = "allegro-pl";
    private static final Money BARGAIN = Money.of("19.99", "PLN");
    private static final int LIMIT_PER_USER = 3;
    private static final BigDecimal DECLARED_STOCK = new BigDecimal("7");

    @Test
    void applicationRequest_whenRequiredFieldsOnly_leavesOptionalsNull() {
        BadgeApplicationRequest request = BadgeApplicationRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .build();

        assertEquals(CAMPAIGN_ID, request.campaignId());
        assertEquals(OFFER_ID, request.offerId());
        assertNull(request.bargainPrice());
        assertNull(request.purchaseLimitPerUser());
        assertNull(request.declaredStockQuantity());
    }

    @Test
    void applicationRequest_whenAllFieldsSet_retainsEveryValue() {
        BadgeApplicationRequest request = BadgeApplicationRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .bargainPrice(BARGAIN)
                .purchaseLimitPerUser(LIMIT_PER_USER)
                .declaredStockQuantity(DECLARED_STOCK)
                .build();

        assertEquals(BARGAIN, request.bargainPrice());
        assertEquals(LIMIT_PER_USER, request.purchaseLimitPerUser());
        assertEquals(DECLARED_STOCK, request.declaredStockQuantity());
    }

    @Test
    void applicationRequest_toBuilder_preservesAllFields() {
        BadgeApplicationRequest original = BadgeApplicationRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .bargainPrice(BARGAIN)
                .purchaseLimitPerUser(LIMIT_PER_USER)
                .declaredStockQuantity(DECLARED_STOCK)
                .build();

        BadgeApplicationRequest copy = original.toBuilder().build();

        assertEquals(original.campaignId(), copy.campaignId());
        assertEquals(original.offerId(), copy.offerId());
        assertEquals(original.bargainPrice(), copy.bargainPrice());
        assertEquals(original.purchaseLimitPerUser(), copy.purchaseLimitPerUser());
        assertEquals(original.declaredStockQuantity(), copy.declaredStockQuantity());
    }

    @Test
    void applicationRequest_whenCampaignIdMissing_throwsIllegalState() {
        BadgeApplicationRequest.Builder builder = BadgeApplicationRequest.builder().offerId(OFFER_ID);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void applicationRequest_whenOfferIdMissing_throwsIllegalState() {
        BadgeApplicationRequest.Builder builder = BadgeApplicationRequest.builder().campaignId(CAMPAIGN_ID);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void applicationFilter_all_hasNoConstraints() {
        BadgeApplicationFilter filter = BadgeApplicationFilter.all();
        assertNull(filter.campaignId());
        assertNull(filter.offerId());
    }

    @Test
    void applicationFilter_toBuilder_preservesFields() {
        BadgeApplicationFilter original = BadgeApplicationFilter.builder()
                .campaignId(CAMPAIGN_ID)
                .offerId(OFFER_ID)
                .build();

        BadgeApplicationFilter copy = original.toBuilder().build();

        assertEquals(CAMPAIGN_ID, copy.campaignId());
        assertEquals(OFFER_ID, copy.offerId());
    }

    @Test
    void badgeFilter_whenMarketplaceSet_buildsAndPreservesOnToBuilder() {
        BadgeFilter original = BadgeFilter.builder()
                .marketplaceId(MARKETPLACE)
                .offerId(OFFER_ID)
                .build();

        assertEquals(MARKETPLACE, original.marketplaceId());
        assertEquals(OFFER_ID, original.offerId());

        BadgeFilter copy = original.toBuilder().build();
        assertEquals(MARKETPLACE, copy.marketplaceId());
        assertEquals(OFFER_ID, copy.offerId());
    }

    @Test
    void badgeFilter_whenMarketplaceMissing_throwsIllegalState() {
        BadgeFilter.Builder builder = BadgeFilter.builder().offerId(OFFER_ID);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void badgePatch_finish_hasFinishKindAndNoPrice() {
        BadgePatch patch = BadgePatch.finish();
        assertEquals(BadgePatch.Kind.FINISH, patch.kind());
        assertNull(patch.bargainPrice());
    }

    @Test
    void badgePatch_changeBargainPrice_carriesPrice() {
        BadgePatch patch = BadgePatch.changeBargainPrice(BARGAIN);
        assertEquals(BadgePatch.Kind.CHANGE_BARGAIN_PRICE, patch.kind());
        assertEquals(BARGAIN, patch.bargainPrice());
    }

    @Test
    void badgePatch_changeBargainPrice_whenNull_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> BadgePatch.changeBargainPrice(null));
    }

    @Test
    void badgePatch_equalsHashCodeToString_behaveByValue() {
        BadgePatch first = BadgePatch.changeBargainPrice(BARGAIN);
        BadgePatch second = BadgePatch.changeBargainPrice(BARGAIN);
        BadgePatch finish = BadgePatch.finish();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.equals(first));
        assertFalse(first.equals(finish));
        assertFalse(first.equals(null));
        assertFalse(first.equals("not a patch"));
        assertTrue(first.toString().contains(BARGAIN.amount()));
    }
}
