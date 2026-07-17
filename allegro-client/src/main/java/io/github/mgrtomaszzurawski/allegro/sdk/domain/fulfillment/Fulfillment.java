/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;

/**
 * One Fulfillment by Allegro — reached via {@code AllegroClient.fulfillment()}.
 *
 * <p>Operations require the {@code fulfillment:read} / {@code fulfillment:write}
 * OAuth scopes and an account enrolled in One Fulfillment; calls from a
 * non-enrolled account are rejected by Allegro with a typed error.
 *
 * <p>Starter slice of bucket I: only the removal-preference pair ships first, as
 * the end-to-end proof of the bucket's conventions. Advance ship notices, stock,
 * available products, parcels, refund dispositions and tax id follow per the
 * task-division plan.
 *
 * @since 0.2.0
 */
public interface Fulfillment {

    /**
     * The seller's active preference for how removable goods leave the
     * warehouse.
     *
     * @return the current removal preference
     */
    RemovalPreference removalPreference();

    /**
     * Set the seller's active removal preference.
     *
     * @param preference the preference to store
     * @return the stored preference as echoed back by Allegro
     */
    RemovalPreference setRemovalPreference(RemovalPreference preference);
}
