/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsRequestAbroadFreeDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsRequestFreeDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsRequestJoinPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsRequestJoinPolicyRaw.StrategyEnum;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsRequestMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.DeliverySettingsRequestBuilder;
import org.jspecify.annotations.Nullable;

/**
 * A request to replace the seller's delivery settings, assembled with
 * {@link #builder()}. {@code settings().update(...)} has PUT semantics, so build
 * the full desired state — the usual flow is {@code settings().get()}, adjust,
 * then {@code update(...)}. {@code joinPolicy} is required; the free-delivery
 * thresholds and the marketplace are optional.
 *
 * @param marketplaceId the marketplace to apply the settings to, or {@code null}
 *     for the seller's default marketplace
 * @param freeDelivery domestic free-delivery threshold, or {@code null} for none
 * @param abroadFreeDelivery cross-border free-delivery threshold, or {@code null}
 * @param joinPolicy how a multi-item order's delivery cost is combined (required)
 *
 * @since 0.3.0
 */
public record DeliverySettingsRequest(
        @Nullable String marketplaceId,
        @Nullable Money freeDelivery,
        @Nullable Money abroadFreeDelivery,
        JoinStrategy joinPolicy) {

    /** A fresh builder for a {@link DeliverySettingsRequest}. */
    public static DeliverySettingsRequestBuilder builder() {
        return new DeliverySettingsRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public DeliverySettingsRequestBuilder toBuilder() {
        return new DeliverySettingsRequestBuilder()
                .marketplaceId(marketplaceId)
                .freeDelivery(freeDelivery)
                .abroadFreeDelivery(abroadFreeDelivery)
                .joinPolicy(joinPolicy);
    }

    /** Build the generated Layer-1 DTO for the request body. */
    public DeliverySettingsRequestRaw toRaw() {
        DeliverySettingsRequestRaw raw = new DeliverySettingsRequestRaw();
        if (marketplaceId != null) {
            DeliverySettingsRequestMarketplaceRaw marketplace =
                    new DeliverySettingsRequestMarketplaceRaw();
            marketplace.setId(marketplaceId);
            raw.setMarketplace(marketplace);
        }
        if (freeDelivery != null) {
            raw.setFreeDelivery(freeDeliveryRaw(freeDelivery));
        }
        if (abroadFreeDelivery != null) {
            DeliverySettingsRequestAbroadFreeDeliveryRaw abroad =
                    new DeliverySettingsRequestAbroadFreeDeliveryRaw();
            abroad.setAmount(abroadFreeDelivery.amount());
            abroad.setCurrency(abroadFreeDelivery.currency());
            raw.setAbroadFreeDelivery(abroad);
        }
        DeliverySettingsRequestJoinPolicyRaw joinPolicyRaw =
                new DeliverySettingsRequestJoinPolicyRaw();
        joinPolicyRaw.setStrategy(StrategyEnum.fromValue(joinPolicy.wireValue()));
        raw.setJoinPolicy(joinPolicyRaw);
        return raw;
    }

    private static DeliverySettingsRequestFreeDeliveryRaw freeDeliveryRaw(Money money) {
        DeliverySettingsRequestFreeDeliveryRaw raw = new DeliverySettingsRequestFreeDeliveryRaw();
        raw.setAmount(money.amount());
        raw.setCurrency(money.currency());
        return raw;
    }
}
