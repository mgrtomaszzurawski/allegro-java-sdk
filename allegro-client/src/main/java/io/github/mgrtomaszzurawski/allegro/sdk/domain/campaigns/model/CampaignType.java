/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeCampaignRaw;

/**
 * Kind of badge campaign, which determines what a participating offer gains.
 *
 * @since 0.2.0
 */
public enum CampaignType {

    /** The badge grants a discount (e.g. "Strefa Okazji" bargain deals). */
    DISCOUNT,

    /** A standard promotional badge with no price mechanism. */
    STANDARD,

    /** A sourcing campaign the platform runs to grow supply of a product. */
    SOURCING;

    /** Map the generated Layer-1 enum to the public domain enum. */
    static CampaignType from(BadgeCampaignRaw.TypeEnum raw) {
        return switch (raw) {
            case DISCOUNT -> DISCOUNT;
            case STANDARD -> STANDARD;
            case SOURCING -> SOURCING;
        };
    }
}
