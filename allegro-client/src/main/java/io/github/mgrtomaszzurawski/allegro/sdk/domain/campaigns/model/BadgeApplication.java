/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationPurchaseConstraintsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A seller's request to display a campaign badge on one of their offers, and its
 * verification outcome. Created by {@code badges().apply(...)}, listed by
 * {@code badges().streamApplications(...)}, and read by {@code badges().application(id)}.
 *
 * <p>A fresh application is {@link BadgeApplicationStatus#REQUESTED}; Allegro then
 * verifies it (often with an e-mail-notified manual step) and it becomes
 * {@link BadgeApplicationStatus#PROCESSED} or {@link BadgeApplicationStatus#DECLINED}.
 *
 * @param id                   application id
 * @param createdAt            when the application was submitted
 * @param updatedAt            when the application last changed state
 * @param campaignId           the badge campaign applied for
 * @param offerId              the offer the badge is requested on
 * @param status               verification state
 * @param rejectionReasons     why the application was declined; empty unless
 *                             {@code status} is {@link BadgeApplicationStatus#DECLINED}
 * @param bargainPrice         the declared bargain price, or {@code null} if none
 * @param campaignStockQuantity units reserved for the campaign, or {@code null}
 * @param purchaseLimitPerUser per-buyer purchase limit, or {@code null} if unset
 *
 * @since 0.2.0
 */
public record BadgeApplication(
        String id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String campaignId,
        String offerId,
        BadgeApplicationStatus status,
        List<CampaignRefusalReason> rejectionReasons,
        @Nullable Money bargainPrice,
        @Nullable BigDecimal campaignStockQuantity,
        @Nullable Integer purchaseLimitPerUser) {

    public BadgeApplication {
        rejectionReasons = List.copyOf(rejectionReasons);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static BadgeApplication from(BadgeApplicationRaw raw) {
        return new BadgeApplication(
                raw.getId(),
                OffsetDateTime.parse(raw.getCreatedAt()),
                OffsetDateTime.parse(raw.getUpdatedAt()),
                raw.getCampaign().getId(),
                raw.getOffer().getId(),
                BadgeApplicationStatus.from(raw.getProcess().getStatus()),
                CampaignMappers.rejectionReasons(raw.getProcess().getRejectionReasons()),
                bargainPrice(raw),
                campaignStockQuantity(raw),
                purchaseLimitPerUser(raw.getPurchaseConstraints()));
    }

    private static @Nullable Money bargainPrice(BadgeApplicationRaw raw) {
        if (raw.getPrices() == null || raw.getPrices().getBargain() == null) {
            return null;
        }
        return CampaignMappers.nullableMoney(
                raw.getPrices().getBargain().getAmount(), raw.getPrices().getBargain().getCurrency());
    }

    private static @Nullable BigDecimal campaignStockQuantity(BadgeApplicationRaw raw) {
        return raw.getCampaignStock() == null ? null : raw.getCampaignStock().getQuantity();
    }

    private static @Nullable Integer purchaseLimitPerUser(
            @Nullable BadgeApplicationPurchaseConstraintsRaw constraints) {
        if (constraints == null || constraints.getLimit() == null
                || constraints.getLimit().getPerUser() == null) {
            return null;
        }
        return constraints.getLimit().getPerUser().getMaxItems();
    }
}
