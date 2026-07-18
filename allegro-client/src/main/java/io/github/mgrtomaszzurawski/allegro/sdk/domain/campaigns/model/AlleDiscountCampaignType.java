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
    DISCOUNT;

    /** Map the generated Layer-1 type enum to the public enum. */
    static AlleDiscountCampaignType from(
            AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw.TypeEnum wireValue) {
        return valueOf(wireValue.name());
    }
}
