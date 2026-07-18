/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.CommandTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCriteriumRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPriceChangeCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferQuantityChangeCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationFixedPriceHolderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceModificationFixedPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicationChangeCommandDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicationModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.QuantityModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskReportRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.OfferBatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Endpoint wrapper behind the {@link OfferBatch} facade. Each command is a
 * client-generated id PUT to Allegro, polled to a terminal state, then its
 * per-offer task pages are gathered into the returned {@link BatchReport}. Every
 * command shares that submit → poll → gather flow ({@link #submitAndAwait}); only
 * the endpoint and request body differ.
 *
 * @since 0.2.0
 */
public final class OfferBatchImpl implements OfferBatch {

    private static final String OP_PUBLISH = "publish offers";
    private static final String OP_UNPUBLISH = "unpublish offers";
    private static final String OP_CHANGE_PRICES = "change offer prices";
    private static final String OP_CHANGE_QUANTITIES = "change offer quantities";
    private static final String OP_POLL = "await batch command";
    private static final String OP_TASKS = "read batch command tasks";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final int TASKS_PAGE_SIZE = 100;

    private final HttpSupport http;
    private final CommandPoller poller;

    public OfferBatchImpl(HttpRuntime runtime) {
        this(runtime, new CommandPoller());
    }

    /** Test seam: inject a poller with a fast (or no-op) sleeper. */
    public OfferBatchImpl(HttpRuntime runtime, CommandPoller poller) {
        this.http = new HttpSupport(runtime);
        this.poller = poller;
    }

    @Override
    public BatchReport publish(List<String> offerIds) {
        return publication(offerIds, PublicationModificationRaw.ActionEnum.ACTIVATE, OP_PUBLISH);
    }

    @Override
    public BatchReport unpublish(List<String> offerIds) {
        return publication(offerIds, PublicationModificationRaw.ActionEnum.END, OP_UNPUBLISH);
    }

    @Override
    public BatchReport changePrices(List<String> offerIds, Money price) {
        String commandId = UUID.randomUUID().toString();
        OfferPriceChangeCommandRaw body = new OfferPriceChangeCommandRaw()
                .offerCriteria(criteria(offerIds))
                .modification(new PriceModificationFixedPriceRaw().price(
                        new PriceModificationFixedPriceHolderRaw()
                                .amount(price.amount()).currency(price.currency())));
        return submitAndAwait(ApiPaths.offerPriceChangeCommand(commandId),
                ApiPaths.offerPriceChangeCommandTasks(commandId), body, OP_CHANGE_PRICES);
    }

    @Override
    public BatchReport changeQuantities(List<String> offerIds, int quantity) {
        String commandId = UUID.randomUUID().toString();
        OfferQuantityChangeCommandRaw body = new OfferQuantityChangeCommandRaw()
                .offerCriteria(criteria(offerIds))
                .modification(new QuantityModificationRaw()
                        .changeType(QuantityModificationRaw.ChangeTypeEnum.FIXED).value(quantity));
        return submitAndAwait(ApiPaths.offerQuantityChangeCommand(commandId),
                ApiPaths.offerQuantityChangeCommandTasks(commandId), body, OP_CHANGE_QUANTITIES);
    }

    private BatchReport publication(List<String> offerIds,
            PublicationModificationRaw.ActionEnum action, String operationName) {
        String commandId = UUID.randomUUID().toString();
        PublicationChangeCommandDtoRaw body = new PublicationChangeCommandDtoRaw()
                .offerCriteria(criteria(offerIds))
                .publication(new PublicationModificationRaw().action(action));
        return submitAndAwait(ApiPaths.offerPublicationCommand(commandId),
                ApiPaths.offerPublicationCommandTasks(commandId), body, operationName);
    }

    /** Every offer id, wrapped as a single "contains these offers" criterion. */
    private static List<OfferCriteriumRaw> criteria(List<String> offerIds) {
        return List.of(new OfferCriteriumRaw()
                .type(OfferCriteriumRaw.TypeEnum.CONTAINS_OFFERS)
                .offers(offerIds.stream().map(id -> new OfferIdRaw().id(id)).toList()));
    }

    /** Submit a command, wait for it to finish, and gather its per-offer tasks. */
    private BatchReport submitAndAwait(String commandPath, String tasksPath, Object body,
            String operationName) {
        http.request(operationName).put(commandPath).jsonBody(body).send();
        // The command runs asynchronously; wait until Allegro stamps completedAt.
        GeneralReportRaw report = poller.await(
                () -> http.getAuthenticated(commandPath, GeneralReportRaw.class, OP_POLL),
                terminal -> terminal.getCompletedAt() != null,
                operationName);
        return BatchReport.from(report, gatherTasks(tasksPath));
    }

    /** Consume every task page (bounded by the offers submitted) into one list. */
    private List<CommandTaskRaw> gatherTasks(String tasksPath) {
        List<CommandTaskRaw> gathered = new ArrayList<>();
        int offset = 0;
        while (true) {
            TaskReportRaw page = http.request(OP_TASKS)
                    .get(tasksPath)
                    .query(Query.create().add(QUERY_OFFSET, offset).add(QUERY_LIMIT, TASKS_PAGE_SIZE))
                    .fetch(TaskReportRaw.class);
            List<CommandTaskRaw> tasks = page.getTasks();
            if (tasks == null || tasks.isEmpty()) {
                break;
            }
            gathered.addAll(tasks);
            if (tasks.size() < TASKS_PAGE_SIZE) {
                break;
            }
            offset += TASKS_PAGE_SIZE;
        }
        return gathered;
    }
}
