/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders;

import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPickupDropOffPointRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AllegroPickupDropOffPointsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CarrierParcelTrackingResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormAddWaybillCreatedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormOrderWaybillResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventStatsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventsListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrdersShippingCarrierRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrdersShippingCarriersResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.CommissionRefunds;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.CustomerReturns;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.OrderInvoices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.Orders;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.PointsFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.SerialNumbersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Carrier;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.CarrierTracking;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEventStats;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderEventType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.OrderStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.PickupPoint;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Waybill;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrappers behind the {@link Orders} facade (order-management surface).
 *
 * @since 0.3.0
 */
public final class OrdersImpl implements Orders {

    private static final int ORDERS_PAGE_SIZE = 100;
    private static final int EVENTS_PAGE_SIZE = 100;

    private static final String OP_STREAM_ORDERS = "stream orders";
    private static final String OP_GET_ORDER = "get order";
    private static final String OP_STREAM_EVENTS = "stream order events";
    private static final String OP_EVENT_STATS = "get order event stats";
    private static final String OP_MARK_STATUS = "set order fulfillment status";
    private static final String OP_SET_SERIAL_NUMBERS = "set order serial numbers";
    private static final String OP_ATTACH_BILLING_LINK = "attach billing document link";
    private static final String OP_TRACKING_NUMBERS = "list order tracking numbers";
    private static final String OP_ADD_TRACKING_NUMBER = "add order tracking number";
    private static final String OP_CARRIERS = "list carriers";
    private static final String OP_CARRIER_TRACKING = "get carrier tracking";
    private static final String OP_PICKUP_POINTS = "list allegro pickup points";

    private static final String QUERY_STATUS = "status";
    private static final String QUERY_FULFILLMENT_STATUS = "fulfillment.status";
    private static final String QUERY_FULFILLMENT_PROVIDER_ID = "fulfillment.provider.id";
    private static final String QUERY_LINE_ITEMS_SENT = "fulfillment.shipmentSummary.lineItemsSent";
    private static final String QUERY_BOUGHT_GTE = "lineItems.boughtAt.gte";
    private static final String QUERY_BOUGHT_LTE = "lineItems.boughtAt.lte";
    private static final String QUERY_UPDATED_GTE = "updatedAt.gte";
    private static final String QUERY_UPDATED_LTE = "updatedAt.lte";
    private static final String QUERY_BUYER_LOGIN = "buyer.login";
    private static final String QUERY_MARKETPLACE_ID = "marketplace.id";
    private static final String QUERY_PAYMENT_ID = "payment.id";
    private static final String QUERY_SURCHARGE_ID = "surcharges.id";
    private static final String QUERY_DELIVERY_METHOD_ID = "delivery.method.id";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String QUERY_FROM = "from";
    private static final String QUERY_TYPE = "type";
    private static final String QUERY_REVISION = "checkoutForm.revision";
    private static final String QUERY_WAYBILL = "waybill";
    private static final String QUERY_CARRIERS = "carriers";

    private static final String ERR_BLANK_REVISION =
            "revision must not be blank; use the two-argument overload for last-write-wins";

    private final HttpSupport http;
    private final OrderInvoices invoices;
    private final CustomerReturns returns;
    private final CommissionRefunds commissionRefunds;

    public OrdersImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.invoices = new OrderInvoicesImpl(runtime);
        this.returns = new CustomerReturnsImpl(runtime);
        this.commissionRefunds = new CommissionRefundsImpl(runtime);
    }

    @Override
    public OrderInvoices invoices() {
        return invoices;
    }

    @Override
    public CustomerReturns returns() {
        return returns;
    }

    @Override
    public CommissionRefunds commissionRefunds() {
        return commissionRefunds;
    }

    @Override
    public Stream<Order> streamOrders(OrderFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchOrdersPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<Order> fetchOrdersPage(OrderFilter filter, int pageIndex) {
        int offset = pageIndex * ORDERS_PAGE_SIZE;
        Query query = Query.create();
        for (OrderStatus status : filter.statuses()) {
            query.add(QUERY_STATUS, wireValueOf(status));
        }
        for (SellerStatus status : filter.fulfillmentStatuses()) {
            query.add(QUERY_FULFILLMENT_STATUS, wireValueOf(status));
        }
        query.add(QUERY_FULFILLMENT_PROVIDER_ID, filter.fulfillmentProviderId())
                .add(QUERY_LINE_ITEMS_SENT, filter.lineItemsSent())
                .add(QUERY_BOUGHT_GTE, filter.boughtFrom())
                .add(QUERY_BOUGHT_LTE, filter.boughtTo())
                .add(QUERY_UPDATED_GTE, filter.updatedFrom())
                .add(QUERY_UPDATED_LTE, filter.updatedTo())
                .add(QUERY_BUYER_LOGIN, filter.buyerLogin())
                .add(QUERY_MARKETPLACE_ID, filter.marketplaceId())
                .add(QUERY_PAYMENT_ID, filter.paymentId())
                .add(QUERY_SURCHARGE_ID, filter.surchargeId())
                .add(QUERY_DELIVERY_METHOD_ID, filter.deliveryMethodId())
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, ORDERS_PAGE_SIZE);
        CheckoutFormsRaw response = http.request(OP_STREAM_ORDERS)
                .get(ApiPaths.ORDER_CHECKOUT_FORMS)
                .query(query)
                .fetch(CheckoutFormsRaw.class);
        List<CheckoutFormRaw> forms = response.getCheckoutForms();
        List<Order> items = forms == null ? List.of() : forms.stream().map(Order::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    private static boolean hasMore(int offset, int pageCount, @Nullable BigDecimal totalCount) {
        if (totalCount == null) {
            // No total advertised: assume more only while pages come back full.
            return pageCount == ORDERS_PAGE_SIZE;
        }
        return (long) offset + pageCount < totalCount.longValue();
    }

    @Override
    public Order get(String orderId) {
        return Order.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId),
                CheckoutFormRaw.class, OP_GET_ORDER));
    }

    @Override
    public Stream<OrderEvent> streamEvents(OrderEventFilter filter) {
        return PagedSpliterator.cursorStream(cursor -> fetchEventsPage(filter, cursor));
    }

    private PagedSpliterator.CursorPage<OrderEvent> fetchEventsPage(
            OrderEventFilter filter, @Nullable String cursor) {
        Query query = Query.create().add(QUERY_FROM, cursor);
        for (OrderEventType type : filter.types()) {
            query.add(QUERY_TYPE, wireValueOf(type));
        }
        query.add(QUERY_LIMIT, EVENTS_PAGE_SIZE);
        OrderEventsListRaw response = http.request(OP_STREAM_EVENTS)
                .get(ApiPaths.ORDER_EVENTS)
                .query(query)
                .fetch(OrderEventsListRaw.class);
        List<OrderEventRaw> events = response.getEvents();
        List<OrderEvent> items = events == null ? List.of() : events.stream().map(OrderEvent::from).toList();
        // Cursor is the last event id: `from` is exclusive, so the next page
        // starts after it. An empty page ends the walk (null cursor).
        String nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).id();
        return new PagedSpliterator.CursorPage<>(items, nextCursor);
    }

    @Override
    public OrderEventStats eventStats() {
        return OrderEventStats.from(http.getAuthenticated(
                ApiPaths.ORDER_EVENT_STATS, OrderEventStatsRaw.class, OP_EVENT_STATS));
    }

    @Override
    public void markStatus(String orderId, SellerStatus status) {
        putFulfillment(orderId, status, null);
    }

    @Override
    public void markStatus(String orderId, SellerStatus status, String revision) {
        putFulfillment(orderId, status, requireRevision(revision));
    }

    private void putFulfillment(String orderId, SellerStatus status, @Nullable String revision) {
        http.request(OP_MARK_STATUS)
                .put(ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId, ApiPaths.FULFILLMENT_SEGMENT))
                .query(Query.create().add(QUERY_REVISION, revision))
                .jsonBody(OrdersRequestFactory.fulfillment(status))
                .send();
    }

    @Override
    public void setSerialNumbers(String orderId, SerialNumbersRequest request) {
        postSerialNumbers(orderId, request, null);
    }

    @Override
    public void setSerialNumbers(String orderId, SerialNumbersRequest request, String revision) {
        postSerialNumbers(orderId, request, requireRevision(revision));
    }

    // The three-argument overloads promise optimistic concurrency, so a blank
    // revision must fail loudly rather than be silently dropped by Query.add
    // (which would degrade the guarded write to last-write-wins).
    private static String requireRevision(String revision) {
        if (revision == null || revision.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_REVISION);
        }
        return revision;
    }

    private void postSerialNumbers(String orderId, SerialNumbersRequest request, @Nullable String revision) {
        http.request(OP_SET_SERIAL_NUMBERS)
                .post(ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId,
                        ApiPaths.SERIAL_NUMBERS_SEGMENT))
                .query(Query.create().add(QUERY_REVISION, revision))
                .jsonBody(OrdersRequestFactory.serialNumbers(request))
                .send();
    }

    @Override
    public void attachBillingDocumentLink(String orderId, String url) {
        http.request(OP_ATTACH_BILLING_LINK)
                .post(ApiPaths.subPath(ApiPaths.ORDER_ROOT, orderId,
                        ApiPaths.BILLING_DOCUMENTS_SEGMENT, ApiPaths.LINKS_SEGMENT))
                .jsonBody(OrdersRequestFactory.billingDocumentLink(url))
                .send();
    }

    @Override
    public List<Waybill> trackingNumbers(String orderId) {
        CheckoutFormOrderWaybillResponseRaw response = http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId, ApiPaths.SHIPMENTS_SEGMENT),
                CheckoutFormOrderWaybillResponseRaw.class, OP_TRACKING_NUMBERS);
        List<CheckoutFormAddWaybillCreatedRaw> shipments = response.getShipments();
        return shipments == null ? List.of() : shipments.stream().map(Waybill::from).toList();
    }

    @Override
    public Waybill addTrackingNumber(String orderId, ShipmentRequest request) {
        return Waybill.from(http.postJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.ORDER_CHECKOUT_FORMS, orderId, ApiPaths.SHIPMENTS_SEGMENT),
                OrdersRequestFactory.shipment(request),
                CheckoutFormAddWaybillCreatedRaw.class, OP_ADD_TRACKING_NUMBER));
    }

    @Override
    public List<Carrier> carriers() {
        OrdersShippingCarriersResponseRaw response = http.getAuthenticated(
                ApiPaths.ORDER_CARRIERS, OrdersShippingCarriersResponseRaw.class, OP_CARRIERS);
        List<OrdersShippingCarrierRaw> carriers = response.getCarriers();
        return carriers == null ? List.of() : carriers.stream().map(Carrier::from).toList();
    }

    @Override
    public CarrierTracking carrierTracking(String carrierId, String waybill) {
        CarrierParcelTrackingResponseRaw response = http.request(OP_CARRIER_TRACKING)
                .get(ApiPaths.subPath(ApiPaths.ORDER_CARRIERS, carrierId, ApiPaths.TRACKING_SEGMENT))
                .query(Query.create().add(QUERY_WAYBILL, waybill))
                .fetch(CarrierParcelTrackingResponseRaw.class);
        return CarrierTracking.from(response);
    }

    @Override
    public List<PickupPoint> allegroPickupPoints(PointsFilter filter) {
        Query query = Query.create().addAll(QUERY_CARRIERS, filter.carrierCodes());
        AllegroPickupDropOffPointsResponseRaw response = http.request(OP_PICKUP_POINTS)
                .get(ApiPaths.ALLEGRO_PICKUP_POINTS)
                .query(query)
                .fetch(AllegroPickupDropOffPointsResponseRaw.class);
        List<AllegroPickupDropOffPointRaw> points = response.getPoints();
        return points == null ? List.of() : points.stream().map(PickupPoint::from).toList();
    }

    // The UNKNOWN sentinel is a read-only forward-compat value (a wire value this
    // release does not model); it is not a real filter token, so it must never be
    // serialized into a query parameter. Dropping it to null omits the param
    // (Query.add skips nulls) rather than sending status=UNKNOWN, which the server
    // would reject with 400. Mirrors OffersImpl.wireValueOf.

    /** The wire token for an order-status filter, or {@code null} to omit it (never {@code UNKNOWN}). */
    private static @Nullable String wireValueOf(OrderStatus status) {
        return status == OrderStatus.UNKNOWN ? null : status.name();
    }

    /** The wire token for a fulfillment-status filter, or {@code null} to omit it (never {@code UNKNOWN}). */
    private static @Nullable String wireValueOf(SellerStatus status) {
        return status == SellerStatus.UNKNOWN ? null : status.name();
    }

    /** The wire token for an event-type filter, or {@code null} to omit it (never {@code UNKNOWN}). */
    private static @Nullable String wireValueOf(OrderEventType type) {
        return type == OrderEventType.UNKNOWN ? null : type.name();
    }
}
