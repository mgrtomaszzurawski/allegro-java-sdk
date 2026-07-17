/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import java.util.stream.Stream;

/**
 * After-sale service conditions — reached via {@code settings().afterSale()}.
 *
 * <p>Starter slice of bucket K: seller warranty definitions. Implied warranties,
 * return policies and attachments land in the following bucket-K PRs.
 *
 * @since 0.2.0
 */
public interface AfterSaleConditions {

    /**
     * Lazily stream the seller's warranty definitions. Pages are fetched on
     * demand as the stream is consumed; only summaries (id + name) are returned
     * — call {@link #warranty(String)} for the full definition.
     *
     * @return a lazy stream over the seller's warranties
     */
    Stream<WarrantySummary> streamWarranties();

    /**
     * Fetch a single warranty definition in full.
     *
     * @param warrantyId the warranty definition identifier
     * @return the full warranty definition
     */
    Warranty warranty(String warrantyId);

    /**
     * Create a new warranty definition.
     *
     * @param request the validated warranty request
     * @return the created warranty definition, with its server-assigned id
     */
    Warranty createWarranty(WarrantyRequest request);

    /**
     * Replace an existing warranty definition.
     *
     * @param warrantyId the warranty definition identifier
     * @param request the validated warranty request
     * @return the updated warranty definition
     */
    Warranty updateWarranty(String warrantyId, WarrantyRequest request);
}
