/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.fulfillment;

import io.github.mgrtomaszzurawski.allegro.client.model.AdvanceShipNoticeListItemResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdvanceShipNoticeListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdvanceShipNoticeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdvanceShipNoticeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CreateAdvanceShipNoticeRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CreateAdvanceShipNoticeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.HandlingUnitRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReceivingStateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubmitCommandInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubmitCommandOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SubmitCommandRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UpdateSubmittedAdvanceShipNoticeRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.AdvanceShipNotices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.SubmittedAsnUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AdvanceShipNotice;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.HandlingUnit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReceivingState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.SubmitStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Etagged;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrappers behind the {@link AdvanceShipNotices} sub-facade. Drives the
 * shared transport, threads the {@code ETag} optimistic-concurrency token through
 * reads and writes, downloads label PDFs as bytes, and polls the async submit
 * command to a terminal state via {@link CommandPoller}.
 *
 * @since 0.4.0
 */
public final class AdvanceShipNoticesImpl implements AdvanceShipNotices {

    /** ASN list caps {@code limit} at 200; 100 keeps each page small but few. */
    private static final int PAGE_SIZE = 100;
    private static final String MEDIA_PDF = "application/pdf";

    private static final String OP_STREAM = "stream advance ship notices";
    private static final String OP_GET = "get advance ship notice";
    private static final String OP_CREATE = "create advance ship notice";
    private static final String OP_UPDATE = "update advance ship notice";
    private static final String OP_UPDATE_SUBMITTED = "update submitted advance ship notice";
    private static final String OP_SUBMIT = "submit advance ship notice";
    private static final String OP_SUBMIT_POLL = "poll advance ship notice submit command";
    private static final String OP_CANCEL = "cancel advance ship notice";
    private static final String OP_DELETE = "delete advance ship notice";
    private static final String OP_LABELS = "get advance ship notice labels";
    private static final String OP_RECEIVING_STATE = "get advance ship notice receiving state";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String QUERY_STATUS = "status";

    private static final String ERR_FILTER_NULL = "filter must not be null";
    private static final String ERR_ASN_ID_NULL = "asnId must not be null";
    private static final String ERR_REQUEST_NULL = "request must not be null";
    private static final String ERR_UPDATE_NULL = "update must not be null";
    private static final String ERR_VERSION_NULL = "version must not be null";
    private static final String ERR_TIMEOUT_NULL = "timeout must not be null";

    private final HttpSupport http;
    private final CommandPoller commandPoller;

    public AdvanceShipNoticesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.commandPoller = new CommandPoller();
    }

    @Override
    public Stream<AdvanceShipNotice> streamNotices() {
        return streamNotices(AsnFilter.all());
    }

    @Override
    public Stream<AdvanceShipNotice> streamNotices(AsnFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        return PagedSpliterator.stream(pageIndex -> fetchNoticesPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<AdvanceShipNotice> fetchNoticesPage(AsnFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        List<String> statusValues = wireStatuses(filter);
        if (!statusValues.isEmpty()) {
            query = query.addAll(QUERY_STATUS, statusValues);
        }
        AdvanceShipNoticeListRaw response = http.request(OP_STREAM)
                .get(ApiPaths.FULFILLMENT_ADVANCE_SHIP_NOTICES)
                .query(query)
                .fetch(AdvanceShipNoticeListRaw.class);
        List<AdvanceShipNoticeListItemResponseRaw> notices = response.getAdvanceShipNotices();
        List<AdvanceShipNotice> items = notices == null
                ? List.of()
                : notices.stream().map(AdvanceShipNotice::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    @Override
    public AdvanceShipNotice get(String asnId) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        Etagged<AdvanceShipNoticeResponseRaw> response = http.request(OP_GET)
                .get(ApiPaths.advanceShipNotice(asnId))
                .fetchWithETag(AdvanceShipNoticeResponseRaw.class);
        return AdvanceShipNotice.from(response.value(), response.etag());
    }

    @Override
    public AdvanceShipNotice create(AsnRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        Etagged<CreateAdvanceShipNoticeResponseRaw> response = http.request(OP_CREATE)
                .post(ApiPaths.FULFILLMENT_ADVANCE_SHIP_NOTICES)
                .jsonBodyPartial(toCreateRaw(request))
                .fetchWithETag(CreateAdvanceShipNoticeResponseRaw.class);
        return AdvanceShipNotice.from(response.value(), response.etag());
    }

    @Override
    public AdvanceShipNotice update(String asnId, AsnRequest request, String version) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        Objects.requireNonNull(version, ERR_VERSION_NULL);
        Etagged<AdvanceShipNoticeResponseRaw> response = http.request(OP_UPDATE)
                .put(ApiPaths.advanceShipNotice(asnId))
                .ifMatch(version)
                .jsonBodyPartial(toUpdateRaw(request))
                .fetchWithETag(AdvanceShipNoticeResponseRaw.class);
        return AdvanceShipNotice.from(response.value(), response.etag());
    }

    @Override
    public AdvanceShipNotice updateSubmitted(String asnId, SubmittedAsnUpdate update, String version) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        Objects.requireNonNull(update, ERR_UPDATE_NULL);
        Objects.requireNonNull(version, ERR_VERSION_NULL);
        Etagged<AdvanceShipNoticeResponseRaw> response = http.request(OP_UPDATE_SUBMITTED)
                .put(ApiPaths.advanceShipNoticeSubmitted(asnId))
                .ifMatch(version)
                .jsonBodyPartial(toSubmittedRaw(update))
                .fetchWithETag(AdvanceShipNoticeResponseRaw.class);
        return AdvanceShipNotice.from(response.value(), response.etag());
    }

    @Override
    public SubmitStatus submit(String asnId) {
        return runSubmit(asnId, null);
    }

    @Override
    public SubmitStatus submit(String asnId, Duration timeout) {
        Objects.requireNonNull(timeout, ERR_TIMEOUT_NULL);
        return runSubmit(asnId, timeout);
    }

    private SubmitStatus runSubmit(String asnId, @Nullable Duration timeout) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        UUID commandId = UUID.randomUUID();
        SubmitCommandRaw body = new SubmitCommandRaw()
                .id(commandId)
                .input(new SubmitCommandInputRaw().advanceShipNoticeId(UUID.fromString(asnId)));
        http.request(OP_SUBMIT)
                .put(ApiPaths.fulfillmentSubmitCommand(commandId.toString()))
                .jsonBodyPartial(body)
                .fetch(SubmitCommandRaw.class);
        SubmitCommandRaw terminal = await(
                () -> fetchSubmitCommand(commandId.toString()),
                AdvanceShipNoticesImpl::submitTerminal, OP_SUBMIT, timeout);
        return SubmitStatus.fromWire(terminalStatus(terminal));
    }

    private SubmitCommandRaw fetchSubmitCommand(String commandId) {
        return http.request(OP_SUBMIT_POLL)
                .get(ApiPaths.fulfillmentSubmitCommand(commandId))
                .fetch(SubmitCommandRaw.class);
    }

    @Override
    public void cancel(String asnId) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        http.request(OP_CANCEL)
                .put(ApiPaths.advanceShipNoticeCancel(asnId))
                .send();
    }

    @Override
    public void delete(String asnId) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        http.request(OP_DELETE)
                .delete(ApiPaths.advanceShipNotice(asnId))
                .send();
    }

    @Override
    public byte[] labels(String asnId) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        return http.request(OP_LABELS)
                .get(ApiPaths.advanceShipNoticeLabels(asnId))
                .accept(MEDIA_PDF)
                .fetchBytes();
    }

    @Override
    public ReceivingState receivingState(String asnId) {
        Objects.requireNonNull(asnId, ERR_ASN_ID_NULL);
        return ReceivingState.from(http.getAuthenticated(
                ApiPaths.advanceShipNoticeReceivingState(asnId), ReceivingStateRaw.class, OP_RECEIVING_STATE));
    }

    private <S> S await(Supplier<S> fetchStatus, Predicate<S> isTerminal, String operationName,
            @Nullable Duration timeout) {
        return timeout == null
                ? commandPoller.await(fetchStatus, isTerminal, operationName)
                : commandPoller.await(fetchStatus, isTerminal, operationName, timeout);
    }

    /**
     * A submit command is terminal once it stops running: {@code RUNNING} is the
     * only non-terminal state, so anything else (including a status Allegro adds
     * later) ends the poll rather than hanging until timeout.
     */
    private static boolean submitTerminal(SubmitCommandRaw raw) {
        SubmitCommandOutputRaw output = raw.getOutput();
        return output != null && output.getStatus() != null
                && SubmitStatus.fromWire(output.getStatus()) != SubmitStatus.RUNNING;
    }

    private static String terminalStatus(SubmitCommandRaw raw) {
        return Objects.requireNonNull(raw.getOutput()).getStatus();
    }

    private static List<String> wireStatuses(AsnFilter filter) {
        List<String> values = new ArrayList<>();
        for (AsnStatus status : filter.statuses()) {
            String wire = status.wireValue();
            // A null wire value is UNKNOWN — never send the sentinel as a filter (it would 400).
            if (wire != null) {
                values.add(wire);
            }
        }
        return values;
    }

    /** Count-based has-more: another page exists while the walked offset has not yet reached the total. */
    private static boolean hasMore(int offset, int returnedCount, @Nullable BigDecimal totalCount) {
        if (totalCount == null) {
            return returnedCount == PAGE_SIZE;
        }
        return (long) offset + returnedCount < totalCount.longValue();
    }

    private static CreateAdvanceShipNoticeRequestRaw toCreateRaw(AsnRequest request) {
        return new CreateAdvanceShipNoticeRequestRaw()
                .items(toItemRaws(request.items()))
                .handlingUnit(toHandlingUnitRaw(request.handlingUnit()))
                .declaredVolumeInCc(request.declaredVolumeInCc());
    }

    private static AdvanceShipNoticeRaw toUpdateRaw(AsnRequest request) {
        return new AdvanceShipNoticeRaw()
                .items(toItemRaws(request.items()))
                .handlingUnit(toHandlingUnitRaw(request.handlingUnit()))
                .declaredVolumeInCc(request.declaredVolumeInCc());
    }

    private static UpdateSubmittedAdvanceShipNoticeRequestRaw toSubmittedRaw(SubmittedAsnUpdate update) {
        UpdateSubmittedAdvanceShipNoticeRequestRaw raw = new UpdateSubmittedAdvanceShipNoticeRequestRaw()
                .handlingUnit(toHandlingUnitRaw(update.handlingUnit()))
                .declaredVolumeInCc(update.declaredVolumeInCc());
        if (!update.items().isEmpty()) {
            raw.setItems(toItemRaws(update.items()));
        }
        return raw;
    }

    private static List<ProductItemRaw> toItemRaws(List<AsnItem> items) {
        return items.stream().map(AdvanceShipNoticesImpl::toItemRaw).toList();
    }

    private static ProductItemRaw toItemRaw(AsnItem item) {
        return new ProductItemRaw()
                .product(new ProductRaw().id(UUID.fromString(item.productId())))
                .quantity(item.quantity());
    }

    private static @Nullable HandlingUnitRaw toHandlingUnitRaw(@Nullable HandlingUnit handlingUnit) {
        if (handlingUnit == null) {
            return null;
        }
        return new HandlingUnitRaw()
                .unitType(handlingUnit.unitType())
                .amount(handlingUnit.amount())
                .labelsType(handlingUnit.labelsType());
    }
}
