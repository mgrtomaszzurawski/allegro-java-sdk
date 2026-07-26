/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.client.model.CommandTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GeneralReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBulkChangeCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferChangeCommandRaw;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceStockBatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceStockTaskResult;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.BulkOfferModificationMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.OfferModificationMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.PricingRulesMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
    private static final String OP_MODIFY_PRICES_STOCK = "modify offer prices and stock";
    private static final String OP_APPLY_PRICING_RULES = "apply offer pricing rules";
    private static final String OP_MODIFY = "modify offers";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final int TASKS_PAGE_SIZE = 100;

    // Bulk price/stock task JSON keys. The tasks endpoint returns a oneOf of two
    // structurally identical subjects (price vs stock) with no discriminator, so
    // the generated union deserializer over-matches (both branches match →
    // "2 classes match, expected 1"). Both branches carry the same shape, so the
    // tasks page is read from the JSON tree into one unified result instead.
    private static final String JSON_TASKS = "tasks";
    private static final String JSON_SUBJECT = "subject";
    private static final String JSON_OFFER_ID = "offerId";
    private static final String JSON_FIELD = "field";
    private static final String JSON_STATUS = "status";
    private static final String JSON_MESSAGE = "message";

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
        return publication(offerIds, PublicationModificationRaw.ActionEnum.ACTIVATE, null, OP_PUBLISH);
    }

    @Override
    public BatchReport publish(List<String> offerIds, OffsetDateTime scheduledFor) {
        return publication(offerIds, PublicationModificationRaw.ActionEnum.ACTIVATE, scheduledFor, OP_PUBLISH);
    }

    @Override
    public BatchReport unpublish(List<String> offerIds) {
        return publication(offerIds, PublicationModificationRaw.ActionEnum.END, null, OP_UNPUBLISH);
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

    @Override
    public PriceStockBatchReport modifyPricesAndStock(List<BulkPriceStockModification> modifications) {
        UUID commandId = UUID.randomUUID();
        // Each modification may change a price, a stock, or both; Allegro requires
        // one change kind per wire element, so flat-map to the split elements.
        OfferBulkChangeCommandRaw body = new OfferBulkChangeCommandRaw()
                .commandId(commandId)
                .modifications(modifications.stream()
                        .flatMap(modification -> BulkOfferModificationMapper
                                .toWireElements(modification).stream()).toList());
        return submitPostAndAwait(commandId.toString(), body);
    }

    @Override
    public BatchReport modify(BatchModificationRequest modification) {
        String commandId = UUID.randomUUID().toString();
        String commandPath = ApiPaths.offerModificationCommand(commandId);
        OfferChangeCommandRaw body = OfferModificationMapper.toRaw(modification);
        // Partial body: the generated Modification carries ten optional sub-objects,
        // all null but the one set here; a full serialization would send them as
        // null and reset those aspects server-side (KNOWN-SERVER-BEHAVIORS — create).
        http.request(OP_MODIFY).put(commandPath).jsonBodyPartial(body).send();
        return awaitAndGather(commandPath,
                ApiPaths.offerModificationCommandTasks(commandId), OP_MODIFY);
    }

    @Override
    public BatchReport applyPricingRules(BatchPricingRulesRequest request) {
        UUID commandId = UUID.randomUUID();
        OfferAutomaticPricingCommandRaw body = PricingRulesMapper.toRaw(commandId, request);
        return submitCollectionAndAwait(
                ApiPaths.SALE_OFFER_PRICE_AUTOMATION_COMMANDS,
                ApiPaths.offerPriceAutomationCommand(commandId.toString()),
                ApiPaths.offerPriceAutomationCommandTasks(commandId.toString()),
                body, OP_APPLY_PRICING_RULES);
    }

    private BatchReport publication(List<String> offerIds,
            PublicationModificationRaw.ActionEnum action, @Nullable OffsetDateTime scheduledFor,
            String operationName) {
        String commandId = UUID.randomUUID().toString();
        PublicationModificationRaw publication = new PublicationModificationRaw().action(action);
        if (scheduledFor != null) {
            publication.scheduledFor(scheduledFor);
        }
        PublicationChangeCommandDtoRaw body = new PublicationChangeCommandDtoRaw()
                .offerCriteria(criteria(offerIds))
                .publication(publication);
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
        return awaitAndGather(commandPath, tasksPath, operationName);
    }

    /**
     * Poll a submitted command until Allegro stamps {@code completedAt}, then
     * gather every per-offer task page into the terminal {@link BatchReport}.
     * Shared by every submit variant (PUT full/partial, POST-to-collection); only
     * the submit call differs.
     */
    private BatchReport awaitAndGather(String statusPath, String tasksPath, String operationName) {
        GeneralReportRaw report = poller.await(
                () -> http.getAuthenticated(statusPath, GeneralReportRaw.class, OP_POLL),
                terminal -> terminal.getCompletedAt() != null,
                operationName);
        return BatchReport.from(report, gatherTasks(tasksPath));
    }

    /**
     * Submit a command by POSTing to its collection with the client-generated id
     * in the BODY (public media type), poll it to completion, and gather its
     * per-offer tasks. Distinct from {@link #submitAndAwait} (which PUTs a
     * client-id'd resource) and {@link #submitPostAndAwait} (bulk price/stock,
     * which uses the beta media type and reads tree-shaped tasks): this is the
     * plain-{@code public.v1} POST-to-collection variant. The body is serialized
     * partially so an unset optional field (e.g. a rule's price-range
     * configuration) is omitted rather than sent as {@code null}.
     */
    private BatchReport submitCollectionAndAwait(String collectionPath, String statusPath,
            String tasksPath, Object body, String operationName) {
        http.request(operationName).post(collectionPath).jsonBodyPartial(body).send();
        return awaitAndGather(statusPath, tasksPath, operationName);
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

    /**
     * Submit the bulk price/stock command (POST to the collection with the
     * client-generated id in the BODY, beta media type), poll it to completion,
     * and gather its per-field tasks. Distinct from {@link #submitAndAwait}: those
     * commands PUT a client-id'd resource; this one POSTs the collection.
     */
    private PriceStockBatchReport submitPostAndAwait(String commandId, OfferBulkChangeCommandRaw body) {
        // Partial (NON_EMPTY) body: a modification sets a price map, a stock, or
        // both, so unset optional branches must be OMITTED, never sent as
        // null/{} (which the generated DTO pre-initializes and would reset).
        http.request(OP_MODIFY_PRICES_STOCK)
                .post(ApiPaths.SALE_OFFER_BULK_MODIFICATION_COMMANDS)
                .acceptBeta().betaJsonBodyPartial(body).send();
        String statusPath = ApiPaths.offerBulkModificationCommand(commandId);
        GeneralReportRaw report = poller.await(
                () -> http.request(OP_POLL).get(statusPath).acceptBeta().fetch(GeneralReportRaw.class),
                terminal -> terminal.getCompletedAt() != null,
                OP_MODIFY_PRICES_STOCK);
        return PriceStockBatchReport.from(report,
                gatherSubjectTasks(ApiPaths.offerBulkModificationCommandTasks(commandId)));
    }

    /**
     * Consume every task page of a bulk price/stock command into one list. Read
     * from the JSON tree (see the JSON key constants above): the generated union
     * type for these tasks over-matches its two identical branches and fails to
     * deserialize, and both branches carry the same offer/field/status shape.
     */
    private List<PriceStockTaskResult> gatherSubjectTasks(String tasksPath) {
        List<PriceStockTaskResult> gathered = new ArrayList<>();
        int offset = 0;
        while (true) {
            JsonNode page = http.request(OP_TASKS)
                    .get(tasksPath)
                    .acceptBeta()
                    .query(Query.create().add(QUERY_OFFSET, offset).add(QUERY_LIMIT, TASKS_PAGE_SIZE))
                    .fetch(JsonNode.class);
            JsonNode tasks = page.path(JSON_TASKS);
            if (!tasks.isArray() || tasks.isEmpty()) {
                break;
            }
            for (JsonNode task : tasks) {
                JsonNode subject = task.path(JSON_SUBJECT);
                gathered.add(new PriceStockTaskResult(
                        textOrNull(subject.get(JSON_OFFER_ID)),
                        textOrNull(subject.get(JSON_FIELD)),
                        textOrNull(task.get(JSON_STATUS)),
                        textOrNull(task.get(JSON_MESSAGE))));
            }
            if (tasks.size() < TASKS_PAGE_SIZE) {
                break;
            }
            offset += TASKS_PAGE_SIZE;
        }
        return gathered;
    }

    private static @Nullable String textOrNull(@Nullable JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
