/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.payments;

import io.github.mgrtomaszzurawski.allegro.client.model.BaseOperationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetRefundedPayments200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PaymentOperationsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundDetailsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.Payments;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.PaymentOperationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentRefund;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrappers behind the {@link Payments} facade.
 *
 * @since 0.5.0
 */
public final class PaymentsImpl implements Payments {

    private static final int PAGE_SIZE = 100;

    private static final String OP_STREAM_OPERATIONS = "stream payment operations";
    private static final String OP_STREAM_REFUNDS = "stream refunded payments";
    private static final String OP_REFUND = "initiate refund";

    private static final String QUERY_PAYMENT_ID = "payment.id";
    private static final String QUERY_ORDER_ID = "order.id";
    private static final String QUERY_PARTICIPANT_LOGIN = "participant.login";
    private static final String QUERY_OCCURRED_GTE = "occurredAt.gte";
    private static final String QUERY_OCCURRED_LTE = "occurredAt.lte";
    private static final String QUERY_GROUP = "group";
    private static final String QUERY_MARKETPLACE_ID = "marketplaceId";
    private static final String QUERY_CURRENCY = "currency";
    private static final String QUERY_ID = "id";
    private static final String QUERY_STATUS = "status";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public PaymentsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<PaymentOperation> streamOperations(PaymentOperationFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchOperationsPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<PaymentOperation> fetchOperationsPage(
            PaymentOperationFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_PAYMENT_ID, filter.paymentId())
                .add(QUERY_PARTICIPANT_LOGIN, filter.participantLogin())
                .add(QUERY_OCCURRED_GTE, filter.occurredFrom())
                .add(QUERY_OCCURRED_LTE, filter.occurredTo())
                .add(QUERY_GROUP, filter.group())
                .add(QUERY_MARKETPLACE_ID, filter.marketplaceId())
                .add(QUERY_CURRENCY, filter.currency())
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        PaymentOperationsRaw response = http.request(OP_STREAM_OPERATIONS)
                .get(ApiPaths.PAYMENT_OPERATIONS)
                .query(query)
                .fetch(PaymentOperationsRaw.class);
        List<BaseOperationRaw> operations = response.getPaymentOperations();
        List<PaymentOperation> items = operations == null
                ? List.of()
                : operations.stream().map(PaymentOperation::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    @Override
    public Stream<PaymentRefund> streamRefunds(RefundFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchRefundsPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<PaymentRefund> fetchRefundsPage(RefundFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_ID, filter.refundId())
                .add(QUERY_PAYMENT_ID, filter.paymentId())
                .add(QUERY_ORDER_ID, filter.orderId())
                .add(QUERY_OCCURRED_GTE, filter.occurredFrom())
                .add(QUERY_OCCURRED_LTE, filter.occurredTo())
                .add(QUERY_STATUS, filter.status())
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        GetRefundedPayments200ResponseRaw response = http.request(OP_STREAM_REFUNDS)
                .get(ApiPaths.PAYMENT_REFUNDS)
                .query(query)
                .fetch(GetRefundedPayments200ResponseRaw.class);
        List<RefundDetailsRaw> refunds = response.getRefunds();
        List<PaymentRefund> items = refunds == null
                ? List.of()
                : refunds.stream().map(PaymentRefund::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    private static boolean hasMore(int offset, int pageCount, @Nullable Integer totalCount) {
        if (totalCount == null) {
            return pageCount == PAGE_SIZE;
        }
        return offset + pageCount < totalCount;
    }

    @Override
    public PaymentRefund refund(RefundRequest request) {
        return PaymentRefund.from(http.postJsonAuthenticated(
                ApiPaths.PAYMENT_REFUNDS,
                PaymentsRequestFactory.initializeRefund(request),
                RefundDetailsRaw.class, OP_REFUND));
    }
}
