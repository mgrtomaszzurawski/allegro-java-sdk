/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CommandTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskCountRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The result of a completed batch command (e.g. bulk publish/unpublish): the
 * overall success/failure counts plus the per-offer {@link TaskResult}s. The SDK
 * submits the command, waits for it to finish, and gathers every task page
 * before returning this report, so a consumer sees one terminal result.
 *
 * @param id          the command identifier
 * @param createdAt   when the command was accepted, or {@code null} if the report
 *                    type omits it (e.g. a promotion-package command)
 * @param completedAt when the command finished, or {@code null} if the report
 *                    type omits it (e.g. a promotion-package command)
 * @param total       number of offers the command acted on
 * @param success     number of offers processed successfully
 * @param failed      number of offers that failed
 * @param tasks       the per-offer outcomes
 * @since 0.2.0
 */
public record BatchReport(
        @Nullable String id,
        @Nullable OffsetDateTime createdAt,
        @Nullable OffsetDateTime completedAt,
        int total,
        int success,
        int failed,
        List<TaskResult> tasks) {

    public BatchReport {
        tasks = List.copyOf(tasks);
    }

    /** Combine the command's summary report with its gathered task pages. */
    public static BatchReport from(GeneralReportRaw report, List<CommandTaskRaw> rawTasks) {
        TaskCountRaw count = report.getTaskCount();
        List<TaskResult> tasks = rawTasks.stream().map(TaskResult::from).toList();
        return new BatchReport(
                report.getId(),
                report.getCreatedAt(),
                report.getCompletedAt(),
                countOf(count == null ? null : count.getTotal()),
                countOf(count == null ? null : count.getSuccess()),
                countOf(count == null ? null : count.getFailed()),
                tasks);
    }

    private static int countOf(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
