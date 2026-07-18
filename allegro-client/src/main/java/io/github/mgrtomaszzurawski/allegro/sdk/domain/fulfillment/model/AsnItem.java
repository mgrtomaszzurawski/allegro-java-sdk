/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductItemRaw;
import java.math.BigDecimal;

/**
 * One product line of an Advance Ship Notice: a One Fulfillment product and how
 * many units of it the notice covers. Both fields are always present.
 *
 * @param productId the One Fulfillment product identifier (a UUID)
 * @param quantity  the number of units, between 1 and 1&#95;000&#95;000
 *
 * @since 0.4.0
 */
public record AsnItem(String productId, BigDecimal quantity) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static AsnItem from(ProductItemRaw raw) {
        return new AsnItem(raw.getProduct().getId().toString(), raw.getQuantity());
    }
}
