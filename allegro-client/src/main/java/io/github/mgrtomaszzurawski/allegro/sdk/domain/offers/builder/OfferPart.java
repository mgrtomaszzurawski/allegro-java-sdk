/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

/**
 * A selectable part of an offer for the partial read
 * {@code offers().getFields(offerId, OfferPart...)}. Requesting only the parts
 * you need is faster and more reliable than the full offer read.
 *
 * @since 0.5.0
 */
public enum OfferPart {
    /** The available stock ({@code PartialOffer.availableStock}). */
    STOCK,
    /** The Buy Now price, per marketplace ({@code PartialOffer.price} + {@code marketplacePrices}). */
    PRICE
}
