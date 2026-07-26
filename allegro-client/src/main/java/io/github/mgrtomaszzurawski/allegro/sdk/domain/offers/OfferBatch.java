/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceStockBatchReport;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Bulk offer operations — reached via {@code offers().batch()}. Each method
 * submits an Allegro batch command, waits for it to finish, and returns the
 * terminal {@link BatchReport}; the asynchronous command/poll/task-paging
 * mechanics are fully internal (no {@code CompletableFuture} in the surface).
 *
 * @since 0.2.0
 */
public interface OfferBatch {

    /**
     * Publish (activate) the given offers in one command.
     *
     * @param offerIds the offers to publish
     * @return the command report once every offer has been processed
     */
    BatchReport publish(List<String> offerIds);

    /**
     * Schedule the given offers to publish (activate) at a future time, in one command.
     * Scheduling applies to activation only — Allegro ignores a schedule on the end/unpublish
     * action, so there is no scheduled {@code unpublish} counterpart.
     *
     * @param offerIds     the offers to publish
     * @param scheduledFor when the activation should take effect (must be in the future)
     * @return the command report once the schedule has been accepted for every offer
     */
    BatchReport publish(List<String> offerIds, OffsetDateTime scheduledFor);

    /**
     * Unpublish (end) the given offers in one command.
     *
     * @param offerIds the offers to unpublish
     * @return the command report once every offer has been processed
     */
    BatchReport unpublish(List<String> offerIds);

    /**
     * Set a fixed Buy Now price on the given offers in one command.
     *
     * @param offerIds the offers to reprice
     * @param price    the new fixed Buy Now price
     * @return the command report once every offer has been processed
     */
    BatchReport changePrices(List<String> offerIds, Money price);

    /**
     * Set the available quantity of the given offers in one command.
     *
     * @param offerIds the offers to restock
     * @param quantity the new available quantity
     * @return the command report once every offer has been processed
     */
    BatchReport changeQuantities(List<String> offerIds, int quantity);

    /**
     * Apply per-offer Buy Now price and/or stock changes in one command, mixing
     * FIXED / GAIN / PERCENTAGE adjustments across marketplaces. Unlike
     * {@link #changePrices} and {@link #changeQuantities} (which set one fixed
     * value across the given offers), each {@link BulkPriceStockModification}
     * targets a single offer with its own price map and stock change.
     *
     * @param modifications one entry per offer to change (each with at least a
     *     price or a stock change)
     * @return the command report once every modification has been processed
     */
    PriceStockBatchReport modifyPricesAndStock(List<BulkPriceStockModification> modifications);

    /**
     * Assign or remove automatic-pricing rules on the request's offers in one
     * command. An automatic-pricing rule recalculates an offer's Buy Now price to
     * follow the market (e.g. the lowest Allegro price); this command attaches such
     * a rule to — or removes it from — the given offers on one or more
     * marketplaces. Defining the rules themselves is a separate concern (the
     * pricing facade); this only applies existing rules to offers in bulk.
     *
     * @param request the offers and the per-marketplace rule assignments or
     *     removals, built with {@link BatchPricingRulesRequest}
     * @return the command report once every offer has been processed
     */
    BatchReport applyPricingRules(BatchPricingRulesRequest request);

    /**
     * Apply an offer-settings change to the request's offers in one command — the
     * listing duration (a fixed length or unlimited) or the dispatch time. Unlike
     * {@link #changePrices}/{@link #changeQuantities} (which set a single value
     * across offers), the change is described by a {@link BatchModificationRequest},
     * which requires exactly one field to change (Allegro rejects a command whose
     * modification carries more than one element).
     *
     * @param modification the offers and the single field change to apply
     * @return the command report once every offer has been processed
     */
    BatchReport modify(BatchModificationRequest modification);
}
