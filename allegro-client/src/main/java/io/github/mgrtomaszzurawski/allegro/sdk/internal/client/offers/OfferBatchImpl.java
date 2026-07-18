/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.CommandTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCriteriumRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicationChangeCommandDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PublicationModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskReportRaw;
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
 * per-offer task pages are gathered into the returned {@link BatchReport}.
 *
 * @since 0.2.0
 */
public final class OfferBatchImpl implements OfferBatch {

    private static final String OP_PUBLISH = "publish offers";
    private static final String OP_UNPUBLISH = "unpublish offers";
    private static final String OP_POLL = "await publication command";
    private static final String OP_TASKS = "read publication command tasks";

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
        return runCommand(offerIds, PublicationModificationRaw.ActionEnum.ACTIVATE, OP_PUBLISH);
    }

    @Override
    public BatchReport unpublish(List<String> offerIds) {
        return runCommand(offerIds, PublicationModificationRaw.ActionEnum.END, OP_UNPUBLISH);
    }

    private BatchReport runCommand(List<String> offerIds,
            PublicationModificationRaw.ActionEnum action, String operationName) {
        String commandId = UUID.randomUUID().toString();
        String commandPath = ApiPaths.offerPublicationCommand(commandId);
        PublicationChangeCommandDtoRaw body = new PublicationChangeCommandDtoRaw()
                .offerCriteria(List.of(new OfferCriteriumRaw()
                        .type(OfferCriteriumRaw.TypeEnum.CONTAINS_OFFERS)
                        .offers(offerIds.stream().map(id -> new OfferIdRaw().id(id)).toList())))
                .publication(new PublicationModificationRaw().action(action));
        http.request(operationName).put(commandPath).jsonBody(body).send();

        // The command runs asynchronously; wait until Allegro stamps completedAt.
        GeneralReportRaw report = poller.await(
                () -> http.getAuthenticated(commandPath, GeneralReportRaw.class, OP_POLL),
                terminal -> terminal.getCompletedAt() != null,
                operationName);
        return BatchReport.from(report, gatherTasks(commandId));
    }

    /** Consume every task page (bounded by the offers submitted) into one list. */
    private List<CommandTaskRaw> gatherTasks(String commandId) {
        String tasksPath = ApiPaths.offerPublicationCommandTasks(commandId);
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
