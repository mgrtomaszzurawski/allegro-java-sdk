/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyUpdateRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import java.util.stream.Stream;

/**
 * After-sale service conditions — reached via {@code settings().afterSale()}.
 *
 * <p>Covers seller warranties, implied warranties (rękojmia), return policies, and
 * the warranty-document {@link #uploadAttachment(byte[], String) attachments} they
 * reference.
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

    /**
     * Lazily stream the seller's return-policy definitions. Unlike the warranty
     * listings, this endpoint returns full policies (not summaries), so the
     * stream elements are complete {@link ReturnPolicy} records.
     *
     * <p>The endpoint serves a single page (offset capped at 59, limit at 60),
     * so the stream yields at most the first 60 return policies.
     *
     * @return a lazy stream over the seller's return policies (at most 60)
     */
    Stream<ReturnPolicy> streamReturnPolicies();

    /**
     * Fetch a single return-policy definition in full.
     *
     * @param returnPolicyId the return-policy definition identifier
     * @return the full return-policy definition
     */
    ReturnPolicy returnPolicy(String returnPolicyId);

    /**
     * Create a new return-policy definition.
     *
     * @param request the validated return-policy request
     * @return the created return policy, with its server-assigned id
     */
    ReturnPolicy createReturnPolicy(ReturnPolicyRequest request);

    /**
     * Replace an existing return-policy definition. The {@code fulfillment} flag
     * is fixed at creation and cannot be changed, so it is absent from the update
     * request.
     *
     * @param returnPolicyId the return-policy definition identifier
     * @param request the validated return-policy update request
     * @return the updated return-policy definition
     */
    ReturnPolicy updateReturnPolicy(String returnPolicyId, ReturnPolicyUpdateRequest request);

    /**
     * Delete a return-policy definition. The server returns the deleted policy;
     * the SDK discards that body.
     *
     * @param returnPolicyId the return-policy definition identifier
     */
    void deleteReturnPolicy(String returnPolicyId);

    /**
     * Upload a warranty-document attachment. The SDK declares the attachment under
     * the given file name (obtaining its id) and uploads the file bytes in one call,
     * returning the hosted attachment whose {@link AfterSalesAttachment#id() id} can
     * be referenced from a {@code WarrantyRequest}.
     *
     * @param fileName the file name to register the attachment under (e.g.
     *     {@code warranty.pdf}); Allegro requires it on the declare step
     * @param content the file bytes (e.g. a PDF)
     * @param contentType the file's MIME type (e.g. {@code application/pdf})
     * @return the uploaded attachment (id, name, url)
     */
    AfterSalesAttachment uploadAttachment(String fileName, byte[] content, String contentType);
}
