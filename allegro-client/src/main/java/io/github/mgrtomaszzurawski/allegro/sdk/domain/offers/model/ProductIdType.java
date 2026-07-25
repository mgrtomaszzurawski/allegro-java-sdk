/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferRaw;

/**
 * How a product is identified when binding it to an offer's {@code productSet}. Absent means
 * the {@code productId} is an Allegro catalogue product id; set it to reference the product by
 * a manufacturer identifier instead.
 *
 * @since 0.5.0
 */
public enum ProductIdType {

    /** A GTIN (global trade item number, e.g. an EAN/UPC barcode). */
    GTIN,
    /** An MPN (manufacturer part number). */
    MPN;

    /** Map to the generated product id-type. */
    public ProductOfferRaw.IdTypeEnum toRaw() {
        return this == GTIN ? ProductOfferRaw.IdTypeEnum.GTIN : ProductOfferRaw.IdTypeEnum.MPN;
    }
}
