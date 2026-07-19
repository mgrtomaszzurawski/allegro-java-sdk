/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelPageSize;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelSummaryReport;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link LabelRequest}. At least one shipment id is
 * required; the page size, cut-line marks and summary report are optional.
 *
 * @since 0.4.0
 */
public final class LabelRequestBuilder {

    private static final String FIELD_SHIPMENT_IDS = "LabelRequest.shipmentIds";

    private @Nullable List<String> shipmentIds;
    private @Nullable LabelPageSize pageSize;
    private @Nullable Boolean cutLine;
    private @Nullable LabelSummaryReport summaryReport;

    /** The shipments to render labels for (required; at least one). A defensive copy is taken. */
    public LabelRequestBuilder shipmentIds(@Nullable List<String> value) {
        this.shipmentIds = value == null ? null : List.copyOf(value);
        return this;
    }

    /** The paper size (optional). */
    public LabelRequestBuilder pageSize(@Nullable LabelPageSize value) {
        this.pageSize = value;
        return this;
    }

    /** Whether to print cut-line marks (optional). */
    public LabelRequestBuilder cutLine(@Nullable Boolean value) {
        this.cutLine = value;
        return this;
    }

    /** An optional summary page (optional). */
    public LabelRequestBuilder summaryReport(@Nullable LabelSummaryReport value) {
        this.summaryReport = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link LabelRequest}.
     *
     * @throws IllegalStateException if no shipment id is set
     */
    public LabelRequest build() {
        List<String> validShipmentIds =
                BuilderValidation.requireNonEmpty(shipmentIds, FIELD_SHIPMENT_IDS);
        return new LabelRequest(validShipmentIds, pageSize, cutLine, summaryReport);
    }
}
