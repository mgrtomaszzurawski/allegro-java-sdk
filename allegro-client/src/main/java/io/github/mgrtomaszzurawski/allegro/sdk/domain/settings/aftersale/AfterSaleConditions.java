/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import java.util.stream.Stream;

/**
 * After-sale service conditions — reached via {@code settings().afterSale()}.
 *
 * <p>Covers seller warranties and implied warranties (rękojmia). Return policies
 * and warranty attachments land in the following bucket-K PRs.
 *
 * @since 0.2.0
 */
public interface AfterSaleConditions {

    /**
     * Lazily stream the seller's warranty definitions. Pages are fetched on
     * demand as the stream is consumed; only summaries (id + name) are returned
     * — call {@link #warranty(String)} for the full definition.
     *
     * <p>The endpoint serves a single page: it caps {@code offset} at 59 and
     * {@code limit} at 60, so the stream yields at most the first 60 warranties.
     * A seller's warranty definitions are a small dictionary, so in practice
     * that is the full set.
     *
     * @return a lazy stream over the seller's warranties (at most 60)
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

    /**
     * Lazily stream the seller's implied-warranty (rękojmia) definitions. Pages
     * are fetched on demand; only summaries (id + name) are returned — call
     * {@link #impliedWarranty(String)} for the full definition.
     *
     * <p>The endpoint serves a single page (offset capped at 59, limit at 60),
     * so the stream yields at most the first 60 implied warranties.
     *
     * @return a lazy stream over the seller's implied warranties (at most 60)
     */
    Stream<ImpliedWarrantySummary> streamImpliedWarranties();

    /**
     * Fetch a single implied-warranty definition in full.
     *
     * @param impliedWarrantyId the implied-warranty definition identifier
     * @return the full implied-warranty definition
     */
    ImpliedWarranty impliedWarranty(String impliedWarrantyId);

    /**
     * Create a new implied-warranty definition.
     *
     * @param request the validated implied-warranty request
     * @return the created implied warranty, with its server-assigned id
     */
    ImpliedWarranty createImpliedWarranty(ImpliedWarrantyRequest request);

    /**
     * Replace an existing implied-warranty definition.
     *
     * @param impliedWarrantyId the implied-warranty definition identifier
     * @param request the validated implied-warranty request
     * @return the updated implied-warranty definition
     */
    ImpliedWarranty updateImpliedWarranty(String impliedWarrantyId, ImpliedWarrantyRequest request);
}
