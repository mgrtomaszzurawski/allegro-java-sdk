/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationFixedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationGainRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationPercentageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplacePriceModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBulkModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBulkModificationStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockModificationFixedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockModificationGainRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.PriceChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.StockChange;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the generated bulk price/stock request elements from the SDK's
 * {@link BulkPriceStockModification}. Kept in the Layer-2 {@code mapping/} package
 * (like {@code OfferRequestMapper}) so the wire shape — and the generated
 * {@code *Raw} DTOs — never leak onto the Layer-3 builder's public surface.
 *
 * <p>Allegro requires each {@code modifications[]} element to carry <em>exactly
 * one</em> of {@code prices} or {@code stock} (live-verified: a combined element
 * is rejected with {@code INVALID_SINGLE_ELEMENT_IN_MODIFICATION}), so an offer
 * that changes both is emitted as two elements with the same {@code offerId}.
 */
public final class BulkOfferModificationMapper {

    private BulkOfferModificationMapper() {
    }

    /** The one or two wire elements (price element, then stock element) for one modification. */
    public static List<OfferBulkModificationRaw> toWireElements(BulkPriceStockModification modification) {
        List<OfferBulkModificationRaw> elements = new ArrayList<>();
        Map<String, PriceChange> prices = modification.prices();
        if (!prices.isEmpty()) {
            Map<String, MarketplacePriceModificationRaw> rawPrices = new LinkedHashMap<>();
            prices.forEach((marketplace, change) -> rawPrices.put(marketplace, priceRaw(change)));
            elements.add(new OfferBulkModificationRaw().offerId(modification.offerId()).prices(rawPrices));
        }
        StockChange stock = modification.stock();
        if (stock != null) {
            elements.add(new OfferBulkModificationRaw().offerId(modification.offerId()).stock(stockRaw(stock)));
        }
        return elements;
    }

    private static MarketplacePriceModificationRaw priceRaw(PriceChange change) {
        return switch (change.kind()) {
            case FIXED -> new MarketplacePriceModificationFixedRaw().value(price(change.amount()));
            case GAIN -> new MarketplacePriceModificationGainRaw().value(price(change.amount()));
            case PERCENTAGE -> new MarketplacePriceModificationPercentageRaw().percentage(change.percentage());
        };
    }

    private static OfferBulkModificationStockRaw stockRaw(StockChange change) {
        return change.kind() == StockChange.Kind.FIXED
                ? new StockModificationFixedRaw().value(change.value())
                : new StockModificationGainRaw().value(change.value());
    }

    private static PriceRaw price(Money money) {
        return new PriceRaw().amount(money.amount()).currency(money.currency());
    }
}
