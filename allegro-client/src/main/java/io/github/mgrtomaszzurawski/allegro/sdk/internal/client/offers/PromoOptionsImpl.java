/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackagesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsForSellerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPromoOptionsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoGeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoModificationReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoModificationTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsModificationsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.PromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoOptionModification;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.PromoBatchMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link PromoOptions} facade.
 *
 * @since 0.2.0
 */
public final class PromoOptionsImpl implements PromoOptions {

    private static final String OP_AVAILABLE = "get available promotion packages";
    private static final String OP_FOR_ALL = "stream all offers' promotion packages";
    private static final String OP_FOR_OFFER = "get offer promotion packages";
    private static final String OP_MODIFY = "modify offer promotion packages";
    private static final String OP_MODIFY_BATCH = "modify promotion packages in batch";
    private static final String OP_POLL = "await promo-options command";
    private static final String OP_TASKS = "read promo-options command tasks";

    /** 100 balances round-trips against payload size (well within the endpoint's limit). */
    private static final int PAGE_SIZE = 100;
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String ERR_NO_CHANGES = "at least one promo-option change is required";

    private final HttpSupport http;
    private final CommandPoller poller;

    public PromoOptionsImpl(HttpRuntime runtime) {
        this(runtime, new CommandPoller());
    }

    /** Test seam: inject a poller with a fast (or no-op) sleeper. */
    public PromoOptionsImpl(HttpRuntime runtime, CommandPoller poller) {
        this.http = new HttpSupport(runtime);
        this.poller = poller;
    }

    @Override
    public AvailablePromotionPackages availablePackages() {
        return AvailablePromotionPackages.from(http.getAuthenticated(
                ApiPaths.OFFER_PROMOTION_PACKAGES, AvailablePromotionPackagesRaw.class, OP_AVAILABLE));
    }

    @Override
    public Stream<OfferPromoOptions> forAllOffers() {
        return PagedSpliterator.stream(this::fetchPage);
    }

    private PagedSpliterator.Page<OfferPromoOptions> fetchPage(int pageIndex) {
        Query query = Query.create()
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        OfferPromoOptionsForSellerRaw response = http.request(OP_FOR_ALL)
                .get(ApiPaths.SALE_OFFERS_PROMO_OPTIONS)
                .query(query)
                .fetch(OfferPromoOptionsForSellerRaw.class);
        List<OfferPromoOptionsRaw> promoOptions = response.getPromoOptions();
        List<OfferPromoOptions> items = promoOptions == null
                ? List.of()
                : promoOptions.stream().map(OfferPromoOptions::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public OfferPromoOptions forOffer(String offerId) {
        return OfferPromoOptions.from(http.getAuthenticated(
                ApiPaths.offerPromoOptions(offerId), OfferPromoOptionsRaw.class, OP_FOR_OFFER));
    }

    @Override
    public void modify(String offerId, List<PromoOptionModification> changes) {
        Objects.requireNonNull(offerId, "offerId");
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException(ERR_NO_CHANGES);
        }
        PromoOptionsModificationsRaw body = new PromoOptionsModificationsRaw()
                .modifications(changes.stream().map(PromoOptionModification::toRaw).toList());
        http.request(OP_MODIFY)
                .post(ApiPaths.offerPromoOptionsModification(offerId))
                .jsonBody(body)
                .send();
    }

    @Override
    public BatchReport modifyBatch(BatchPromoOptionsRequest request) {
        String commandId = UUID.randomUUID().toString();
        String commandPath = ApiPaths.offerPromoOptionsCommand(commandId);
        PromoOptionsCommandRaw body = PromoBatchMapper.toRaw(request);
        // Partial body: omitting extraPackages preserves the offers' existing extras
        // (an empty list would not clear them — the builder forbids an empty change).
        http.request(OP_MODIFY_BATCH).put(commandPath).jsonBodyPartial(body).send();
        // PromoGeneralReport has no completedAt; wait until every counted task is terminal.
        PromoGeneralReportRaw report = poller.await(
                () -> http.getAuthenticated(commandPath, PromoGeneralReportRaw.class, OP_POLL),
                PromoBatchMapper::isComplete,
                OP_MODIFY_BATCH);
        return PromoBatchMapper.toReport(report,
                gatherTasks(ApiPaths.offerPromoOptionsCommandTasks(commandId)));
    }

    /** Consume every task page (bounded by the offers submitted) into one list. */
    private List<PromoModificationTaskRaw> gatherTasks(String tasksPath) {
        List<PromoModificationTaskRaw> gathered = new ArrayList<>();
        int offset = 0;
        while (true) {
            PromoModificationReportRaw page = http.request(OP_TASKS)
                    .get(tasksPath)
                    .query(Query.create().add(QUERY_OFFSET, offset).add(QUERY_LIMIT, PAGE_SIZE))
                    .fetch(PromoModificationReportRaw.class);
            List<PromoModificationTaskRaw> tasks = page.getTasks();
            if (tasks == null || tasks.isEmpty()) {
                break;
            }
            gathered.addAll(tasks);
            if (tasks.size() < PAGE_SIZE) {
                break;
            }
            offset += PAGE_SIZE;
        }
        return gathered;
    }
}
