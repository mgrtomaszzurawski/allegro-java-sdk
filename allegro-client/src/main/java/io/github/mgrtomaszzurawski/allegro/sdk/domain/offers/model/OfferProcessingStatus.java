/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferStatusResponseOperationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferStatusResponseRaw;
import org.jspecify.annotations.Nullable;

/**
 * The processing status of an asynchronous offer {@code POST}/{@code PATCH} operation — used to
 * check whether a create/edit that the server accepted for background processing has finished.
 *
 * @param offerId     the offer the operation applies to, or {@code null} if omitted
 * @param operationId the operation id, or {@code null} if omitted
 * @param status      the processing status (Allegro's wire value: {@code PENDING} /
 *                    {@code IN_PROGRESS} / {@code COMPLETED}), or {@code null} if omitted
 * @param startedAt   when processing started, or {@code null} if omitted
 * @since 0.4.0
 */
public record OfferProcessingStatus(
        @Nullable String offerId,
        @Nullable String operationId,
        @Nullable String status,
        @Nullable String startedAt) {

    /** Project a generated processing-status response onto the consumer record. */
    public static OfferProcessingStatus from(SaleProductOfferStatusResponseRaw raw) {
        OfferIdRaw offer = raw.getOffer();
        SaleProductOfferStatusResponseOperationRaw operation = raw.getOperation();
        return new OfferProcessingStatus(
                offer == null ? null : offer.getId(),
                operation == null ? null : operation.getId(),
                operation == null || operation.getStatus() == null
                        ? null : operation.getStatus().getValue(),
                operation == null ? null : operation.getStartedAt());
    }
}
