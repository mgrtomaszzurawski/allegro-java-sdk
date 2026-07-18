/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsResponseAbroadFreeDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsResponseFreeDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsResponseJoinPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DeliverySettingsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * The seller's delivery settings as returned by {@code shipping.settings().get()}.
 * A read view — modify it through a {@link DeliverySettingsRequest} on
 * {@code settings().update(...)}. Named {@code DeliverySettingsView} to leave the
 * {@code DeliverySettings} name to the sub-facade interface.
 *
 * @param marketplaceId the marketplace these settings apply to, or {@code null}
 * @param freeDelivery order-value threshold above which domestic delivery is free,
 *     or {@code null} when the seller offers none
 * @param abroadFreeDelivery threshold above which cross-border delivery is free,
 *     or {@code null} when the seller offers none
 * @param joinPolicy how the delivery cost of a multi-item order is combined, or
 *     {@code null} when the server omits it
 * @param updatedAt when the settings were last changed (ISO-8601 string), or
 *     {@code null}
 *
 * @since 0.3.0
 */
public record DeliverySettingsView(
        @Nullable String marketplaceId,
        @Nullable Money freeDelivery,
        @Nullable Money abroadFreeDelivery,
        @Nullable JoinStrategy joinPolicy,
        @Nullable String updatedAt) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static DeliverySettingsView from(DeliverySettingsResponseRaw raw) {
        DeliverySettingsResponseFreeDeliveryRaw free = raw.getFreeDelivery();
        DeliverySettingsResponseAbroadFreeDeliveryRaw abroad = raw.getAbroadFreeDelivery();
        return new DeliverySettingsView(
                raw.getMarketplace() == null ? null : raw.getMarketplace().getId(),
                free == null ? null : money(free.getAmount(), free.getCurrency()),
                abroad == null ? null : money(abroad.getAmount(), abroad.getCurrency()),
                joinPolicy(raw.getJoinPolicy()),
                raw.getUpdatedAt());
    }

    private static @Nullable Money money(@Nullable String amount, @Nullable String currency) {
        return amount == null || currency == null ? null : Money.of(amount, currency);
    }

    private static @Nullable JoinStrategy joinPolicy(
            @Nullable DeliverySettingsResponseJoinPolicyRaw raw) {
        if (raw == null || raw.getStrategy() == null) {
            return null;
        }
        return JoinStrategy.fromWire(raw.getStrategy().getValue());
    }
}
