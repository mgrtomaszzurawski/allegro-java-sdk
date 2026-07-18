/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgePublicationTimePolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeRaw;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A campaign badge granted to one of the seller's offers, as returned by
 * {@code badges().streamBadges(...)}. Unlike a {@link BadgeApplication} (the
 * request), a {@code Badge} is the live result: its {@link #status()} tracks the
 * display lifecycle and {@link #publication()} states when it shows to buyers.
 *
 * @param offerId              the offer carrying the badge
 * @param campaignId           the badge campaign
 * @param campaignName         the campaign's display name
 * @param status               display lifecycle state
 * @param rejectionReasons     why the badge was declined; empty unless
 *                             {@code status} is {@link BadgeStatus#DECLINED}
 * @param publication          when the badge is shown to buyers, or {@code null}
 * @param prices               the badge's prices, or {@code null} if none apply
 * @param campaignStockQuantity units reserved for the campaign, or {@code null}
 *
 * @since 0.2.0
 */
public record Badge(
        String offerId,
        String campaignId,
        String campaignName,
        BadgeStatus status,
        List<CampaignRefusalReason> rejectionReasons,
        @Nullable CampaignSchedule publication,
        @Nullable BadgePrices prices,
        @Nullable BigDecimal campaignStockQuantity) {

    public Badge {
        rejectionReasons = List.copyOf(rejectionReasons);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Badge from(BadgeRaw raw) {
        return new Badge(
                raw.getOffer().getId(),
                raw.getCampaign().getId(),
                raw.getCampaign().getName(),
                BadgeStatus.from(raw.getProcess().getStatus()),
                CampaignMappers.rejectionReasons(raw.getProcess().getRejectionReasons()),
                publication(raw.getPublication()),
                raw.getPrices() == null ? null : BadgePrices.from(raw.getPrices()),
                raw.getCampaignStock() == null ? null : raw.getCampaignStock().getQuantity());
    }

    private static @Nullable CampaignSchedule publication(@Nullable BadgePublicationTimePolicyRaw policy) {
        return policy == null
                ? null
                : CampaignSchedule.from(policy.getType().getValue(), policy.getFrom(), policy.getTo());
    }
}
