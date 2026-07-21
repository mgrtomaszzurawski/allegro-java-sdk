/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.GeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskCountRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The result of a completed bulk price/stock modification command: the overall
 * total/success/failure counts plus the per-field {@link PriceStockTaskResult}s.
 * The SDK submits the command, waits for it to finish, and gathers every task
 * page before returning this one terminal report.
 *
 * @param id      the command identifier
 * @param total   number of field modifications the command acted on
 * @param success number processed successfully
 * @param failed  number that failed
 * @param tasks   the per-field outcomes
 * @since 0.5.0
 */
public record PriceStockBatchReport(
        @Nullable String id,
        int total,
        int success,
        int failed,
        List<PriceStockTaskResult> tasks) {

    public PriceStockBatchReport {
        tasks = List.copyOf(tasks);
    }

    /** Combine the command's summary report with its gathered task pages. */
    public static PriceStockBatchReport from(GeneralReportRaw report, List<PriceStockTaskResult> tasks) {
        TaskCountRaw count = report.getTaskCount();
        return new PriceStockBatchReport(
                report.getId(),
                countOf(count == null ? null : count.getTotal()),
                countOf(count == null ? null : count.getSuccess()),
                countOf(count == null ? null : count.getFailed()),
                tasks);
    }

    private static int countOf(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
