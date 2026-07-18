/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.PickupPoint;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Waybill;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orders for the authenticated seller — reached via {@code AllegroClient.orders()}:
 * listing and reading orders, advancing their seller-side handling status,
 * managing serial numbers and parcel tracking, and reading the order event log.
 *
 * <p>Covers the order-management surface of bucket B. Customer returns, commission
 * refunds, invoices, payments and billing land in later slices per the
 * task-division plan.
 *
 * @since 0.3.0
 */
public interface Orders {

    /**
     * Lazily stream the seller's orders matching {@code filter}, newest pages
     * fetched on demand. Only one page is held in memory at a time.
     *
     * @param filter the order filter ({@link OrderFilter#all()} for every order)
     * @return a lazy stream of orders
     * @since 0.4.0
     */
    Stream<Order> streamOrders(OrderFilter filter);

    /**
     * Fetch a single order by its identifier.
     *
     * @param orderId the order (checkout form) identifier
     * @return the order details
     * @throws io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException
     *     if no order with that id exists for the authenticated seller
     */
    Order get(String orderId);

    /**
     * Lazily stream the seller's order event log in chronological order, resuming
     * from the last event seen. Use {@link #eventStats()} to discover the newest
     * event before deciding how far to read.
     *
     * @param filter the event filter ({@link OrderEventFilter#all()} for all types)
     * @return a lazy stream of order events
     * @since 0.4.0
     */
    Stream<OrderEvent> streamEvents(OrderEventFilter filter);

    /**
     * Read the latest-order-event marker (the id and time of the newest event).
     *
     * @return the event statistics; its fields are {@code null} when the seller
     *     has no order events yet
     * @since 0.4.0
     */
    OrderEventStats eventStats();

    /**
     * Set the seller-side handling status of an order (last-write-wins).
     *
     * <p>Accepts any {@link SellerStatus}; the server decides which transitions
     * are valid for an order and rejects an illegal one with
     * {@link io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException}
     * (some values such as {@code CANCELLED} may not be seller-settable).
     *
     * @param orderId the order identifier
     * @param status the new seller status
     * @since 0.4.0
     */
    void markStatus(String orderId, SellerStatus status);

    /**
     * Set the seller-side handling status of an order, guarded by the order
     * revision for optimistic concurrency — the write fails if the order changed
     * since {@code revision} was read.
     *
     * @param orderId the order identifier
     * @param status the new seller status
     * @param revision the {@link Order#revision()} last read for this order;
     *     must be non-blank (use {@link #markStatus(String, SellerStatus)} for
     *     last-write-wins)
     * @throws IllegalArgumentException if {@code revision} is blank
     * @since 0.4.0
     */
    void markStatus(String orderId, SellerStatus status, String revision);

    /**
     * Set serial numbers for an order's line items (last-write-wins).
     *
     * @param orderId the order identifier
     * @param request the per-line-item serial numbers
     * @since 0.4.0
     */
    void setSerialNumbers(String orderId, SerialNumbersRequest request);

    /**
     * Set serial numbers for an order's line items, guarded by the order revision
     * for optimistic concurrency.
     *
     * @param orderId the order identifier
     * @param request the per-line-item serial numbers
     * @param revision the {@link Order#revision()} last read for this order;
     *     must be non-blank (use {@link #setSerialNumbers(String, SerialNumbersRequest)}
     *     for last-write-wins)
     * @throws IllegalArgumentException if {@code revision} is blank
     * @since 0.4.0
     */
    void setSerialNumbers(String orderId, SerialNumbersRequest request, String revision);

    /**
     * Attach a link to an externally hosted billing document (e.g. an invoice PDF)
     * to an order.
     *
     * @param orderId the order identifier
     * @param url the publicly reachable document URL
     * @since 0.4.0
     */
    void attachBillingDocumentLink(String orderId, String url);

    /**
     * List the parcel tracking numbers already registered against an order.
     *
     * @param orderId the order identifier
     * @return the registered waybills; never {@code null}, possibly empty
     * @since 0.4.0
     */
    List<Waybill> trackingNumbers(String orderId);

    /**
     * Register a parcel tracking number against an order.
     *
     * @param orderId the order identifier
     * @param request the tracking number to register
     * @return the created waybill, including the id Allegro assigned it
     * @since 0.4.0
     */
    Waybill addTrackingNumber(String orderId, ShipmentRequest request);

    /**
     * List the shipping carriers available for parcel tracking numbers.
     *
     * @return the carrier dictionary; never {@code null}
     * @since 0.4.0
     */
    List<Carrier> carriers();

    /**
     * Read a carrier's delivery-tracking history for a waybill.
     *
     * @param carrierId the carrier identifier
     * @param waybill the carrier's tracking (waybill) number
     * @return the tracking history
     * @since 0.4.0
     */
    CarrierTracking carrierTracking(String carrierId, String waybill);

    /**
     * List Allegro pickup / drop-off points, optionally filtered by carrier.
     *
     * @param filter the points filter ({@link PointsFilter#all()} for all carriers)
     * @return the matching points; never {@code null}
     * @since 0.4.0
     */
    List<PickupPoint> allegroPickupPoints(PointsFilter filter);

    /**
     * Customer invoices on orders (list, declare, upload the file).
     *
     * @return the invoices sub-facade
     * @since 0.6.0
     */
    OrderInvoices invoices();

    /**
     * Customer returns (BETA): browse buyer returns and reject a refund.
     *
     * @return the customer-returns sub-facade
     * @since 0.6.0
     */
    CustomerReturns returns();

    /**
     * Commission-refund claims: file, browse and cancel commission refunds.
     *
     * @return the commission-refunds sub-facade
     * @since 0.6.0
     */
    CommissionRefunds commissionRefunds();
}
