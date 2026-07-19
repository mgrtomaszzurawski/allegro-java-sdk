/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsView;

/**
 * The seller's delivery settings — reached via {@code shipping.settings()}: the
 * free-delivery thresholds and the multi-item join policy. Reads need
 * {@code sale:settings:read}; {@link #update(DeliverySettingsRequest)} needs
 * {@code sale:settings:write}.
 *
 * @since 0.3.0
 */
public interface DeliverySettings {

    /**
     * Read the seller's current delivery settings for their default marketplace.
     *
     * @return the current settings
     */
    DeliverySettingsView get();

    /**
     * Replace the seller's delivery settings (PUT semantics — send the full
     * desired state).
     *
     * @param request the new settings
     * @return the settings as stored after the update
     */
    DeliverySettingsView update(DeliverySettingsRequest request);
}
