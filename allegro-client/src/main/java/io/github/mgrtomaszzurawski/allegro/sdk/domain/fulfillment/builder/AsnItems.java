/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnItem;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared fail-fast validation for Advance Ship Notice product lines, used by the
 * {@link AsnRequest} and {@link SubmittedAsnUpdate} builders so both enforce the
 * spec's {@code quantity} bounds and the product-id format identically.
 * Package-private — not part of the public surface.
 */
final class AsnItems {

    static final int MIN_QUANTITY = 1;
    static final int MAX_QUANTITY = 1_000_000;
    private static final String ERR_PRODUCT_ID_BLANK = "productId must not be blank";
    private static final String ERR_PRODUCT_ID_NOT_UUID = "productId must be a One Fulfillment product UUID";
    private static final String ERR_QUANTITY_RANGE = "quantity must be between 1 and 1000000";

    private AsnItems() {
    }

    /**
     * Validate a product line and return it, or throw if the id is blank / not a
     * UUID, or the quantity is out of range — so a bad line fails fast at the
     * builder rather than deep in request serialization.
     */
    static AsnItem checked(String productId, BigDecimal quantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(ERR_PRODUCT_ID_BLANK);
        }
        try {
            UUID.fromString(productId);
        } catch (IllegalArgumentException notAUuid) {
            throw new IllegalArgumentException(ERR_PRODUCT_ID_NOT_UUID, notAUuid);
        }
        if (quantity == null
                || quantity.compareTo(BigDecimal.valueOf(MIN_QUANTITY)) < 0
                || quantity.compareTo(BigDecimal.valueOf(MAX_QUANTITY)) > 0) {
            throw new IllegalArgumentException(ERR_QUANTITY_RANGE);
        }
        return new AsnItem(productId, quantity);
    }
}
