/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.LabelRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.LabelRequestBuilder;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The request describing which shipments' labels to render and how. At least one
 * shipment id is required; the page size, the cut-line marks and the summary
 * report are optional. The rendered labels are returned as raw bytes (see
 * {@code shipping().labels(...)}).
 *
 * @param shipmentIds the shipments to render labels for (at least one)
 * @param pageSize the paper size, or {@code null} for the carrier default
 * @param cutLine whether to print cut-line marks, or {@code null}
 * @param summaryReport an optional summary page, or {@code null}
 *
 * @since 0.4.0
 */
public record LabelRequest(
        List<String> shipmentIds,
        @Nullable LabelPageSize pageSize,
        @Nullable Boolean cutLine,
        @Nullable LabelSummaryReport summaryReport) {

    /** Canonical constructor: defensively copy the shipment-id list. */
    public LabelRequest {
        shipmentIds = List.copyOf(shipmentIds);
    }

    /** A fresh builder for a {@link LabelRequest}. */
    public static LabelRequestBuilder builder() {
        return new LabelRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public LabelRequestBuilder toBuilder() {
        return new LabelRequestBuilder()
                .shipmentIds(shipmentIds)
                .pageSize(pageSize)
                .cutLine(cutLine)
                .summaryReport(summaryReport);
    }

    /** Build the generated Layer-1 DTO for the request body. */
    public LabelRequestDtoRaw toRaw() {
        LabelRequestDtoRaw raw = new LabelRequestDtoRaw();
        raw.setShipmentIds(List.copyOf(shipmentIds));
        if (pageSize != null) {
            raw.setPageSize(LabelRequestDtoRaw.PageSizeEnum.fromValue(pageSize.wireValue()));
        }
        if (cutLine != null) {
            raw.setCutLine(cutLine);
        }
        if (summaryReport != null) {
            raw.setSummaryReport(summaryReport.toRaw());
        }
        return raw;
    }
}
