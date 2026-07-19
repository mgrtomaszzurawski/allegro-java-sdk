/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw;

/**
 * The kind of AlleDiscount campaign.
 *
 * @since 0.2.0
 */
public enum AlleDiscountCampaignType {

    /** A sourcing campaign. */
    SOURCING,

    /** A discount campaign. */
    DISCOUNT,

    /** A value Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the generated Layer-1 type enum to the public enum, degrading a value
     * Allegro added after this SDK version to {@link #UNKNOWN} rather than failing
     * the read.
     */
    static AlleDiscountCampaignType from(
            AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw.TypeEnum wireValue) {
        try {
            return valueOf(wireValue.name());
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
