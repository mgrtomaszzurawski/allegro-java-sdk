/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.CancelShipmentCommandStatusDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CreateShipmentCommandStatusDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.Error400Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentCancelCommandDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentCancelRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentCreateCommandDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShipmentIdsDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.DeliverySettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.PointsOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.Shipping;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.ShippingRates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryMethod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Shipment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Entry point behind the {@link Shipping} facade. Root-level reads (delivery
 * methods) and the shipment-management operations run through the shared
 * {@link HttpSupport}; the sub-facades are stateless views over the shared
 * runtime, so each accessor returns a fresh instance rather than caching (and
 * exposing) a mutable field. The one piece of retained state is the
 * {@link SellerIdResolver}, held here so its cached {@code GET /me} lookup is
 * shared across those fresh sub-facades (it is never exposed — {@link #points()}
 * passes only its {@code sellerId} supplier).
 *
 * <p>Shipment create and cancel are asynchronous command endpoints (submit →
 * poll status → resolve); the shared {@link CommandPoller} turns them into
 * blocking calls so no command handle leaks into the public surface.
 *
 * @since 0.2.0
 */
public final class ShippingImpl implements Shipping {

    private static final String OP_DELIVERY_METHODS = "list delivery methods";
    private static final String OP_CREATE_SHIPMENT = "create shipment";
    private static final String OP_CREATE_SHIPMENT_POLL = "poll shipment creation";
    private static final String OP_CANCEL_SHIPMENT = "cancel shipment";
    private static final String OP_CANCEL_SHIPMENT_POLL = "poll shipment cancellation";
    private static final String OP_GET_SHIPMENT = "get shipment";
    private static final String OP_LABELS = "render shipment labels";
    private static final String OP_PROTOCOL = "render shipment protocol";

    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String ERR_CREATE_FAILED =
            "Shipment creation command finished with a non-success status (%s)";
    private static final String ERR_CANCEL_FAILED =
            "Shipment cancellation command finished with a non-success status (%s)";
    private static final String ERR_NO_SHIPMENT_IDS = "At least one shipment id is required";
    private static final String ERR_NO_SHIPMENT_ID = "shipmentId is required";
    private static final String EMPTY = "";
    /**
     * Status code carried by the exception for an asynchronous command that
     * reports a terminal {@code ERROR}: the poll response itself was HTTP 200, so
     * there is no transport error status — the failure is the command's own
     * result, and its detail travels in the parsed {@code errors[]}.
     */
    private static final int ASYNC_COMMAND_NO_HTTP_STATUS = 0;

    private final HttpRuntime runtime;
    private final HttpSupport http;
    private final SellerIdResolver sellerIdResolver;
    private final CommandPoller commandPoller;

    public ShippingImpl(HttpRuntime runtime) {
        this.runtime = runtime;
        this.http = new HttpSupport(runtime);
        // One resolver per client so the seller-id lookup is cached across the
        // fresh sub-facade instances that points() hands out.
        this.sellerIdResolver = new SellerIdResolver(runtime);
        this.commandPoller = new CommandPoller();
    }

    @Override
    public List<DeliveryMethod> deliveryMethods() {
        GetListOfDeliveryMethodsUsingGET200ResponseRaw response = http.getAuthenticated(
                ApiPaths.DELIVERY_METHODS, GetListOfDeliveryMethodsUsingGET200ResponseRaw.class,
                OP_DELIVERY_METHODS);
        return mapMethods(response.getDeliveryMethods());
    }

    @Override
    public Shipment createShipment(ShipmentRequest request) {
        return runCreate(request, null);
    }

    @Override
    public Shipment createShipment(ShipmentRequest request, Duration timeout) {
        return runCreate(request, timeout);
    }

    private Shipment runCreate(ShipmentRequest request, @Nullable Duration timeout) {
        ShipmentCreateCommandDtoRaw command = new ShipmentCreateCommandDtoRaw();
        command.setInput(request.toRaw());
        ShipmentCreateCommandDtoRaw accepted = http.postJsonAuthenticated(
                ApiPaths.SHIPMENT_CREATE_COMMANDS, command, ShipmentCreateCommandDtoRaw.class,
                OP_CREATE_SHIPMENT);
        String commandId = accepted.getCommandId();
        CreateShipmentCommandStatusDtoRaw status = await(
                () -> pollCreate(commandId), ShippingImpl::createTerminal,
                OP_CREATE_SHIPMENT, timeout);
        if (status.getStatus() != CreateShipmentCommandStatusDtoRaw.StatusEnum.SUCCESS) {
            throw commandFailure(ERR_CREATE_FAILED.formatted(status.getStatus()), status.getErrors());
        }
        return getShipment(status.getShipmentId());
    }

    private CreateShipmentCommandStatusDtoRaw pollCreate(String commandId) {
        return http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.SHIPMENT_CREATE_COMMANDS, commandId),
                CreateShipmentCommandStatusDtoRaw.class, OP_CREATE_SHIPMENT_POLL);
    }

    @Override
    public Shipment getShipment(String shipmentId) {
        requireShipmentId(shipmentId);
        ShipmentDtoRaw raw = http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.SHIPMENTS, shipmentId), ShipmentDtoRaw.class,
                OP_GET_SHIPMENT);
        return Shipment.from(raw);
    }

    @Override
    public void cancelShipment(String shipmentId) {
        runCancel(shipmentId, null);
    }

    @Override
    public void cancelShipment(String shipmentId, Duration timeout) {
        runCancel(shipmentId, timeout);
    }

    private void runCancel(String shipmentId, @Nullable Duration timeout) {
        requireShipmentId(shipmentId);
        ShipmentCancelRequestDtoRaw input = new ShipmentCancelRequestDtoRaw();
        input.setShipmentId(shipmentId);
        ShipmentCancelCommandDtoRaw command = new ShipmentCancelCommandDtoRaw();
        command.setInput(input);
        ShipmentCancelCommandDtoRaw accepted = http.postJsonAuthenticated(
                ApiPaths.SHIPMENT_CANCEL_COMMANDS, command, ShipmentCancelCommandDtoRaw.class,
                OP_CANCEL_SHIPMENT);
        String commandId = accepted.getCommandId();
        CancelShipmentCommandStatusDtoRaw status = await(
                () -> pollCancel(commandId), ShippingImpl::cancelTerminal,
                OP_CANCEL_SHIPMENT, timeout);
        if (status.getStatus() != CancelShipmentCommandStatusDtoRaw.StatusEnum.SUCCESS) {
            throw commandFailure(ERR_CANCEL_FAILED.formatted(status.getStatus()), status.getErrors());
        }
    }

    private CancelShipmentCommandStatusDtoRaw pollCancel(String commandId) {
        return http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.SHIPMENT_CANCEL_COMMANDS, commandId),
                CancelShipmentCommandStatusDtoRaw.class, OP_CANCEL_SHIPMENT_POLL);
    }

    @Override
    public byte[] labels(LabelRequest request) {
        return http.request(OP_LABELS)
                .post(ApiPaths.SHIPMENT_LABEL)
                .jsonBody(request.toRaw())
                .accept(OCTET_STREAM)
                .fetchBytes();
    }

    @Override
    public byte[] protocol(String... shipmentIds) {
        if (shipmentIds == null || shipmentIds.length == 0) {
            throw new IllegalArgumentException(ERR_NO_SHIPMENT_IDS);
        }
        ShipmentIdsDtoRaw body = new ShipmentIdsDtoRaw();
        body.setShipmentIds(List.of(shipmentIds));
        return http.request(OP_PROTOCOL)
                .post(ApiPaths.SHIPMENT_PROTOCOL)
                .jsonBody(body)
                .accept(OCTET_STREAM)
                .fetchBytes();
    }

    @Override
    public PointsOfService points() {
        return new PointsOfServiceImpl(runtime, sellerIdResolver::sellerId);
    }

    @Override
    public DeliverySettings settings() {
        return new DeliverySettingsImpl(runtime);
    }

    @Override
    public ShippingRates rates() {
        return new ShippingRatesImpl(runtime);
    }

    private <S> S await(Supplier<S> fetchStatus, Predicate<S> isTerminal, String operationName,
            @Nullable Duration timeout) {
        return timeout == null
                ? commandPoller.await(fetchStatus, isTerminal, operationName)
                : commandPoller.await(fetchStatus, isTerminal, operationName, timeout);
    }

    private static void requireShipmentId(String shipmentId) {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException(ERR_NO_SHIPMENT_ID);
        }
    }

    /**
     * Build the exception for an asynchronous command that reached a terminal
     * {@code ERROR}, carrying the command's own {@code errors[]} as typed field
     * errors so the failure is actionable (remediation: fix the request).
     */
    private static AllegroBadRequestException commandFailure(String message,
            @Nullable List<Error400Raw> errors) {
        return new AllegroBadRequestException(
                message, ASYNC_COMMAND_NO_HTTP_STATUS, null, mapFieldErrors(errors));
    }

    private static List<AllegroFieldError> mapFieldErrors(@Nullable List<Error400Raw> errors) {
        if (errors == null) {
            return List.of();
        }
        return errors.stream()
                .map(error -> new AllegroFieldError(
                        error.getCode() == null ? EMPTY : error.getCode(),
                        error.getMessage() == null ? EMPTY : error.getMessage(),
                        error.getUserMessage(), error.getPath(), error.getDetails()))
                .toList();
    }

    /** A create command is terminal once it stops reporting {@code IN_PROGRESS}. */
    private static boolean createTerminal(CreateShipmentCommandStatusDtoRaw status) {
        return status.getStatus() != null
                && status.getStatus() != CreateShipmentCommandStatusDtoRaw.StatusEnum.IN_PROGRESS;
    }

    /** A cancel command is terminal once it stops reporting {@code IN_PROGRESS}. */
    private static boolean cancelTerminal(CancelShipmentCommandStatusDtoRaw status) {
        return status.getStatus() != null
                && status.getStatus() != CancelShipmentCommandStatusDtoRaw.StatusEnum.IN_PROGRESS;
    }

    private static List<DeliveryMethod> mapMethods(
            @Nullable List<GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(DeliveryMethod::from).toList();
    }
}
