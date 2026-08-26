/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormLineItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LineItemDepositRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferReferenceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One purchased position on an order: a quantity of a single offer at the price
 * paid, together with its tax, discounts, selected additional services and any
 * deposit.
 *
 * @param id line-item identifier
 * @param offerId identifier of the purchased offer
 * @param offerName offer title at the time of purchase
 * @param offerExternalId the seller's external (signature) id for the offer, or
 *     {@code null} when not set
 * @param offerHsNumber the offer's HS (customs) number, or {@code null} when not set
 * @param quantity number of units bought
 * @param price unit price paid (already reflecting any discount)
 * @param originalPrice unit price before any discount, or {@code null} when not set
 * @param deposit the deposit charged for the item, or {@code null} when none
 * @param taxInfo the tax applied to the item, or {@code null} when not set
 * @param selectedAdditionalServices the additional services the buyer selected;
 *     never {@code null}, possibly empty
 * @param discounts the discounts applied to the item; never {@code null}, possibly empty
 * @param boughtAt when this position was bought, or {@code null} when absent
 *
 * @since 0.3.0
 */
public record LineItem(
        String id,
        String offerId,
        String offerName,
        @Nullable String offerExternalId,
        @Nullable String offerHsNumber,
        int quantity,
        Money price,
        @Nullable Money originalPrice,
        @Nullable Money deposit,
        @Nullable LineItemTax taxInfo,
        List<LineItemAdditionalService> selectedAdditionalServices,
        List<LineItemDiscount> discounts,
        @Nullable OffsetDateTime boughtAt) {

    /** Canonical constructor — defensively copies the additional-service and discount lists. */
    public LineItem {
        selectedAdditionalServices = selectedAdditionalServices == null
                ? List.of() : List.copyOf(selectedAdditionalServices);
        discounts = discounts == null ? List.of() : List.copyOf(discounts);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static LineItem from(CheckoutFormLineItemRaw raw) {
        OfferReferenceRaw offer = raw.getOffer();
        return new LineItem(
                raw.getId().toString(),
                offer.getId(),
                offer.getName(),
                offer.getExternal() == null ? null : offer.getExternal().getId(),
                offer.getHsNumber(),
                raw.getQuantity().intValueExact(),
                Money.of(raw.getPrice().getAmount(), raw.getPrice().getCurrency()),
                Prices.money(raw.getOriginalPrice()),
                depositMoney(raw.getDeposit()),
                LineItemTax.from(raw.getTax()),
                raw.getSelectedAdditionalServices() == null ? List.of()
                        : raw.getSelectedAdditionalServices().stream()
                                .map(LineItemAdditionalService::from).toList(),
                raw.getDiscounts() == null ? List.of()
                        : raw.getDiscounts().stream().map(LineItemDiscount::from).toList(),
                raw.getBoughtAt());
    }

    private static @Nullable Money depositMoney(@Nullable LineItemDepositRaw raw) {
        return raw == null ? null : Prices.money(raw.getPrice());
    }
}
