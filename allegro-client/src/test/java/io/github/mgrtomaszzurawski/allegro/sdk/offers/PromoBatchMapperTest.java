/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoGeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoModificationTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskCountRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PromoModificationTiming;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.PromoBatchMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Wire-shape mapping of {@link PromoBatchMapper}: the command body's base/extra
 * packages, timing and criterion; the {@code taskCount}-based completion check
 * (there is no {@code completedAt}); and the terminal report mapping. Body
 * assertions are on the serialized JSON tree; NON_EMPTY mirrors the SDK's partial
 * write body so unset optional fields are omitted.
 */
class PromoBatchMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private static final String OFFER_ONE = "111";
    private static final String OFFER_TWO = "222";
    private static final String BASE_PACKAGE = "emphasized1d";
    private static final String EXTRA_PACKAGE = "bold30d";

    private static JsonNode tree(BatchPromoOptionsRequest request) {
        return MAPPER.valueToTree(PromoBatchMapper.toRaw(request));
    }

    @Test
    void toRaw_whenBaseAndExtraAndTiming_buildsFullModificationAndCriteria() {
        // given — base + extra package, timed to the end of cycle, two offers
        BatchPromoOptionsRequest request = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE, OFFER_TWO))
                .basePackage(BASE_PACKAGE)
                .addExtraPackage(EXTRA_PACKAGE)
                .timing(PromoModificationTiming.END_OF_CYCLE)
                .build();

        // when
        JsonNode tree = tree(request);

        // then — the modification and the CONTAINS_OFFERS criterion are on the wire
        assertEquals(BASE_PACKAGE, tree.at("/modification/basePackage/id").asText());
        assertEquals(EXTRA_PACKAGE, tree.at("/modification/extraPackages/0/id").asText());
        assertEquals("END_OF_CYCLE", tree.at("/modification/modificationTime").asText());
        assertEquals("CONTAINS_OFFERS", tree.at("/offerCriteria/0/type").asText());
        assertEquals(OFFER_ONE, tree.at("/offerCriteria/0/offers/0/id").asText());
        assertEquals(OFFER_TWO, tree.at("/offerCriteria/0/offers/1/id").asText());
    }

    @Test
    void toRaw_whenNowTiming_mapsTimingToken() {
        // given/when — the immediate timing (the other enum branch)
        BatchPromoOptionsRequest request = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE))
                .basePackage(BASE_PACKAGE)
                .timing(PromoModificationTiming.NOW)
                .build();

        // then
        assertEquals("NOW", tree(request).at("/modification/modificationTime").asText());
    }

    @Test
    void toRaw_whenBaseOnly_omitsExtraPackagesAndTiming() {
        // given — a base-package-only change
        BatchPromoOptionsRequest request = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE))
                .basePackage(BASE_PACKAGE)
                .build();

        // when
        JsonNode tree = tree(request);

        // then — extraPackages and modificationTime are omitted (partial body)
        assertEquals(BASE_PACKAGE, tree.at("/modification/basePackage/id").asText());
        assertTrue(tree.at("/modification/extraPackages").isMissingNode());
        assertTrue(tree.at("/modification/modificationTime").isMissingNode());
    }

    @Test
    void isComplete_whenAllTasksTerminal_isTrue() {
        // given — every counted task done (success + failed == total)
        PromoGeneralReportRaw report = new PromoGeneralReportRaw()
                .taskCount(new TaskCountRaw().total(2).success(1).failed(1));

        // then
        assertTrue(PromoBatchMapper.isComplete(report));
    }

    @Test
    void isComplete_whenTasksStillRunning_isFalse() {
        // given — not every counted task finished yet
        PromoGeneralReportRaw report = new PromoGeneralReportRaw()
                .taskCount(new TaskCountRaw().total(2).success(0).failed(0));

        // then
        assertFalse(PromoBatchMapper.isComplete(report));
    }

    @Test
    void isComplete_whenTaskCountAbsent_isFalse() {
        // given — the summary has not populated its task count yet
        assertFalse(PromoBatchMapper.isComplete(new PromoGeneralReportRaw()));
    }

    @Test
    void toReport_whenSummaryAndTasks_mapsCountsAndPerOfferTasks() {
        // given — a terminal summary and two tasks (one DONE, one ERROR with a message)
        PromoGeneralReportRaw report = new PromoGeneralReportRaw()
                .id("cmd-1")
                .taskCount(new TaskCountRaw().total(2).success(1).failed(1));
        PromoModificationTaskRaw done = new PromoModificationTaskRaw()
                .offer(new OfferIdRaw().id(OFFER_ONE))
                .status(PromoModificationTaskRaw.StatusEnum.DONE);
        PromoModificationTaskRaw failed = new PromoModificationTaskRaw()
                .offer(new OfferIdRaw().id(OFFER_TWO))
                .status(PromoModificationTaskRaw.StatusEnum.ERROR)
                .errors(List.of(new ErrorRaw().message("package unavailable")));

        // when
        BatchReport batchReport = PromoBatchMapper.toReport(report, List.of(done, failed));

        // then — counts from taskCount, tasks projected onto TaskResult
        assertEquals("cmd-1", batchReport.id());
        // the promotion-package report type carries no timestamps (see PromoBatchMapper javadoc)
        assertNull(batchReport.createdAt());
        assertNull(batchReport.completedAt());
        assertEquals(2, batchReport.total());
        assertEquals(1, batchReport.success());
        assertEquals(1, batchReport.failed());
        assertEquals(OFFER_ONE, batchReport.tasks().get(0).offerId());
        assertEquals("DONE", batchReport.tasks().get(0).status());
        assertNull(batchReport.tasks().get(0).message());
        // the successful task carries no errors and no field (promo is not field-scoped)
        assertTrue(batchReport.tasks().get(0).errors().isEmpty());
        assertNull(batchReport.tasks().get(0).field());
        assertEquals("ERROR", batchReport.tasks().get(1).status());
        assertEquals("package unavailable", batchReport.tasks().get(1).message());
        // and the failed task exposes the same message as a structured typed error
        assertEquals(1, batchReport.tasks().get(1).errors().size());
        assertEquals("package unavailable", batchReport.tasks().get(1).errors().get(0).message());
    }
}
