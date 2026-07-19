/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.JoinStrategy;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link DeliverySettingsRequest}. {@code joinPolicy} is
 * required; the free-delivery thresholds and the marketplace are optional.
 *
 * @since 0.3.0
 */
public final class DeliverySettingsRequestBuilder {

    private static final String FIELD_JOIN_POLICY = "DeliverySettingsRequest.joinPolicy";

    private @Nullable String marketplaceId;
    private @Nullable Money freeDelivery;
    private @Nullable Money abroadFreeDelivery;
    private @Nullable JoinStrategy joinPolicy;

    /** Marketplace to apply the settings to (optional; default marketplace when omitted). */
    public DeliverySettingsRequestBuilder marketplaceId(@Nullable String value) {
        this.marketplaceId = value;
        return this;
    }

    /** Domestic free-delivery threshold (optional). */
    public DeliverySettingsRequestBuilder freeDelivery(@Nullable Money value) {
        this.freeDelivery = value;
        return this;
    }

    /** Cross-border free-delivery threshold (optional). */
    public DeliverySettingsRequestBuilder abroadFreeDelivery(@Nullable Money value) {
        this.abroadFreeDelivery = value;
        return this;
    }

    /** How a multi-item order's delivery cost is combined (required). */
    public DeliverySettingsRequestBuilder joinPolicy(@Nullable JoinStrategy value) {
        this.joinPolicy = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link DeliverySettingsRequest}.
     *
     * @throws IllegalStateException if {@code joinPolicy} is missing
     */
    public DeliverySettingsRequest build() {
        JoinStrategy validJoinPolicy = BuilderValidation.requirePresent(joinPolicy, FIELD_JOIN_POLICY);
        return new DeliverySettingsRequest(
                marketplaceId, freeDelivery, abroadFreeDelivery, validJoinPolicy);
    }
}
