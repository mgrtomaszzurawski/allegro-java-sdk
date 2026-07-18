/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RefundDispositionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.StockFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AvailableProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.FulfillmentOrder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundDisposition;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.StockItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.TaxId;
import java.util.stream.Stream;

/**
 * One Fulfillment by Allegro — reached via {@code AllegroClient.fulfillment()}.
 *
 * <p>Operations require the {@code fulfillment:read} / {@code fulfillment:write}
 * OAuth scopes and an account enrolled in One Fulfillment; calls from a
 * non-enrolled account are rejected by Allegro with a typed error.
 *
 * <p>The {@code stock}, {@code availableProducts} and {@code refundDispositions}
 * reports are lazy {@link Stream}s — pages are fetched only as the stream is
 * consumed. Report records expose optional fields as {@code null} because the
 * server may omit them for a given row.
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

    /**
     * Stream the entire fulfillment stock report.
     *
     * @return a lazy stream over every stock line
     */
    Stream<StockItem> stock();

    /**
     * Stream the fulfillment stock report, filtered.
     *
     * @param filter the filter to apply (use {@link StockFilter#all()} for none)
     * @return a lazy stream over the matching stock lines
     */
    Stream<StockItem> stock(StockFilter filter);

    /**
     * Stream the products the seller may ship into One Fulfillment.
     *
     * @return a lazy stream over every available product
     */
    Stream<AvailableProduct> availableProducts();

    /**
     * Read the parcels shipped from the warehouse for a single order.
     *
     * @param orderId the order to read parcels for
     * @return the order and its shipped parcels
     */
    FulfillmentOrder parcelsOf(String orderId);

    /**
     * Stream the refund-dispositions report for returned or bounced goods.
     *
     * @param filter creation-time bounds (use {@link RefundDispositionFilter#all()} for none)
     * @return a lazy stream over the matching dispositions
     */
    Stream<RefundDisposition> refundDispositions(RefundDispositionFilter filter);

    /**
     * Read the seller's registered tax identification number and its
     * verification status.
     *
     * @return the current tax identification number
     */
    TaxId taxId();

    /**
     * Register the seller's tax identification number.
     *
     * @param taxId the tax identification number to add
     */
    void addTaxId(String taxId);

    /**
     * Replace the seller's registered tax identification number.
     *
     * @param taxId the new tax identification number
     */
    void updateTaxId(String taxId);
}
