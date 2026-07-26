/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping;

import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCriteriumRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoGeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoModificationTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsCommandModificationPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsCommandModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskCountRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PromoModificationTiming;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.TaskResult;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Builds the generated promotion-package command body from the SDK's
 * {@link BatchPromoOptionsRequest} and maps its terminal report + task pages onto
 * the shared {@link BatchReport}. Kept in the Layer-2 {@code mapping/} package so
 * the generated {@code *Raw} DTOs never leak onto the public surface.
 *
 * <p>Unlike the other batch commands, {@code PromoGeneralReport} has no
 * {@code completedAt}; the command is complete once its {@code taskCount} shows
 * every task terminal ({@code success + failed >= total}), which
 * {@link #isComplete} tests.
 */
public final class PromoBatchMapper {

    private PromoBatchMapper() {
    }

    /** The command body for {@code request} (the command id travels in the path, not the body). */
    public static PromoOptionsCommandRaw toRaw(BatchPromoOptionsRequest request) {
        OfferCriteriumRaw criterion = new OfferCriteriumRaw()
                .type(OfferCriteriumRaw.TypeEnum.CONTAINS_OFFERS)
                .offers(request.offerIds().stream().map(id -> new OfferIdRaw().id(id)).toList());
        PromoOptionsCommandModificationRaw modification = new PromoOptionsCommandModificationRaw();
        if (request.basePackageId() != null) {
            modification.basePackage(new PromoOptionsCommandModificationPackageRaw().id(request.basePackageId()));
        }
        if (!request.extraPackageIds().isEmpty()) {
            modification.extraPackages(request.extraPackageIds().stream()
                    .map(id -> new PromoOptionsCommandModificationPackageRaw().id(id)).toList());
        }
        if (request.timing() != null) {
            modification.modificationTime(timingEnum(request.timing()));
        }
        return new PromoOptionsCommandRaw()
                .offerCriteria(List.of(criterion))
                .modification(modification);
    }

    /** Whether the command has finished: every counted task is terminal. */
    public static boolean isComplete(PromoGeneralReportRaw report) {
        TaskCountRaw count = report.getTaskCount();
        if (count == null || count.getTotal() == null) {
            return false;
        }
        return orZero(count.getSuccess()) + orZero(count.getFailed()) >= count.getTotal();
    }

    /** Combine the command's summary with its gathered task pages into a {@link BatchReport}. */
    public static BatchReport toReport(PromoGeneralReportRaw report, List<PromoModificationTaskRaw> tasks) {
        TaskCountRaw count = report.getTaskCount();
        List<TaskResult> results = tasks.stream().map(PromoBatchMapper::taskResult).toList();
        return new BatchReport(
                report.getId(),
                count == null ? 0 : orZero(count.getTotal()),
                count == null ? 0 : orZero(count.getSuccess()),
                count == null ? 0 : orZero(count.getFailed()),
                results);
    }

    private static TaskResult taskResult(PromoModificationTaskRaw task) {
        OfferIdRaw offer = task.getOffer();
        PromoModificationTaskRaw.StatusEnum status = task.getStatus();
        return new TaskResult(
                offer == null ? null : offer.getId(),
                status == null ? null : status.getValue(),
                firstErrorMessage(task.getErrors()));
    }

    private static @Nullable String firstErrorMessage(@Nullable List<ErrorRaw> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        return errors.get(0).getMessage();
    }

    private static PromoOptionsCommandModificationRaw.ModificationTimeEnum timingEnum(
            PromoModificationTiming timing) {
        return timing == PromoModificationTiming.NOW
                ? PromoOptionsCommandModificationRaw.ModificationTimeEnum.NOW
                : PromoOptionsCommandModificationRaw.ModificationTimeEnum.END_OF_CYCLE;
    }

    private static int orZero(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
