/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.LabelRequestDtoSummaryReportRaw;
import java.util.List;
import java.util.Objects;

/**
 * An optional summary page printed alongside a batch of shipping labels: where
 * it is placed and which columns it carries. Request-only — Allegro never reads
 * a summary report back.
 *
 * @param placement where the summary is printed relative to the labels
 * @param fields the columns the summary carries (at least one)
 *
 * @since 0.4.0
 */
public record LabelSummaryReport(
        LabelSummaryPlacement placement,
        List<LabelSummaryField> fields) {

    private static final String ERR_PLACEMENT = "LabelSummaryReport.placement is required";
    private static final String ERR_FIELDS = "LabelSummaryReport.fields is required";

    /** Canonical constructor: require a placement and a non-empty, defensively-copied field list. */
    public LabelSummaryReport {
        Objects.requireNonNull(placement, ERR_PLACEMENT);
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException(ERR_FIELDS);
        }
        fields = List.copyOf(fields);
    }

    /** A summary report with the given placement and columns. */
    public static LabelSummaryReport of(LabelSummaryPlacement placement,
            List<LabelSummaryField> fields) {
        return new LabelSummaryReport(placement, fields);
    }

    /** Build the generated Layer-1 DTO for the request body. */
    public LabelRequestDtoSummaryReportRaw toRaw() {
        LabelRequestDtoSummaryReportRaw raw = new LabelRequestDtoSummaryReportRaw();
        raw.setPlacement(
                LabelRequestDtoSummaryReportRaw.PlacementEnum.fromValue(placement.wireValue()));
        raw.setFields(fields.stream()
                .map(field -> LabelRequestDtoSummaryReportRaw.FieldsEnum.fromValue(field.wireValue()))
                .toList());
        return raw;
    }
}
