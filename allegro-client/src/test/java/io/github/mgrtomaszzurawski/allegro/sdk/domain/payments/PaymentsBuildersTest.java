/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.PaymentOperationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundDeposit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundLineItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundSurcharge;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.RefundReason;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Round-trip and validation tests for the bucket B payments builders. */
class PaymentsBuildersTest {

    private static final String PAYMENT_ID = "0f3e2b1a-1111-2222-3333-444455556666";
    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final String COMMAND_ID = "b1c2d3e4-1111-2222-3333-444455556666";
    private static final String NOT_A_UUID = "not-a-uuid";
    private static final String LOGIN = "test-buyer";
    private static final String CURRENCY = "PLN";
    private static final String GROUP = "REFUND";
    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String STATUS = "SUCCESS";
    private static final String LINE_ITEM_ID = "d4e5f6a7-1111-2222-3333-444455556666";
    private static final String SURCHARGE_ID = "e5f6a7b8-1111-2222-3333-444455556666";
    private static final String AMOUNT = "19.99";
    private static final String DELIVERY_AMOUNT = "9.90";
    private static final String OVERPAID_AMOUNT = "2.00";
    private static final String SERVICES_AMOUNT = "3.50";
    private static final String DEPOSIT_AMOUNT = "1.00";
    private static final String SURCHARGE_AMOUNT = "4.00";
    private static final BigDecimal QUANTITY = new BigDecimal("2");
    private static final String SELLER_COMMENT = "One item out of stock";
    private static final OffsetDateTime FROM =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO =
            OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void refundRequest_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        RefundRequest request = RefundRequest.builder()
                .paymentId(PAYMENT_ID).orderId(ORDER_ID)
                .commandId(COMMAND_ID).reason(RefundReason.COMPLAINT)
                .build();

        // then
        assertEquals(PAYMENT_ID, request.paymentId());
        assertEquals(ORDER_ID, request.orderId());
        assertEquals(COMMAND_ID, request.commandId());
        assertEquals(RefundReason.COMPLAINT, request.reason());

        RefundRequest copy = request.toBuilder().build();
        assertEquals(PAYMENT_ID, copy.paymentId());
        assertEquals(ORDER_ID, copy.orderId());
        assertEquals(COMMAND_ID, copy.commandId());
        assertEquals(RefundReason.COMPLAINT, copy.reason());
    }

    @Test
    void refundRequest_whenPaymentIdMissing_throwsIllegalStateNamingPaymentId() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundRequest.builder().orderId(ORDER_ID).commandId(COMMAND_ID)
                        .reason(RefundReason.REFUND).build());
        assertTrue(failure.getMessage().contains("paymentId"), failure.getMessage());
    }

    @Test
    void refundRequest_whenPaymentIdNotUuid_throwsIllegalStateNamingUuid() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundRequest.builder().paymentId(NOT_A_UUID).orderId(ORDER_ID)
                        .commandId(COMMAND_ID).reason(RefundReason.REFUND).build());
        assertTrue(failure.getMessage().contains("UUID"), failure.getMessage());
    }

    @Test
    void refundRequest_whenOrderIdMissing_throwsIllegalStateNamingOrderId() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundRequest.builder().paymentId(PAYMENT_ID).commandId(COMMAND_ID)
                        .reason(RefundReason.REFUND).build());
        assertTrue(failure.getMessage().contains("orderId"), failure.getMessage());
    }

    @Test
    void refundRequest_whenOrderIdNotUuid_throwsIllegalStateNamingUuid() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundRequest.builder().paymentId(PAYMENT_ID).orderId(NOT_A_UUID)
                        .commandId(COMMAND_ID).reason(RefundReason.REFUND).build());
        assertTrue(failure.getMessage().contains("UUID"), failure.getMessage());
    }

    @Test
    void refundRequest_whenCommandIdMissing_throwsIllegalStateNamingCommandId() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundRequest.builder().paymentId(PAYMENT_ID).orderId(ORDER_ID)
                        .reason(RefundReason.REFUND).build());
        assertTrue(failure.getMessage().contains("commandId"), failure.getMessage());
    }

    @Test
    void refundRequest_whenReasonMissing_throwsIllegalStateNamingReason() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundRequest.builder().paymentId(PAYMENT_ID).orderId(ORDER_ID)
                        .commandId(COMMAND_ID).build());
        assertTrue(failure.getMessage().contains("reason"), failure.getMessage());
    }

    @Test
    void refundRequest_whenPartialComponentsSet_buildsAndToBuilderPreserves() {
        // when — a partial refund carrying every optional component
        RefundRequest request = RefundRequest.builder()
                .paymentId(PAYMENT_ID).orderId(ORDER_ID)
                .commandId(COMMAND_ID).reason(RefundReason.PRODUCT_NOT_AVAILABLE)
                .lineItem(RefundLineItem.byAmount(LINE_ITEM_ID, Money.of(AMOUNT, CURRENCY)))
                .deposit(RefundDeposit.of(LINE_ITEM_ID, Money.of(DEPOSIT_AMOUNT, CURRENCY)))
                .surcharge(RefundSurcharge.of(SURCHARGE_ID, Money.of(SURCHARGE_AMOUNT, CURRENCY)))
                .delivery(Money.of(DELIVERY_AMOUNT, CURRENCY))
                .overpaid(Money.of(OVERPAID_AMOUNT, CURRENCY))
                .additionalServices(Money.of(SERVICES_AMOUNT, CURRENCY))
                .sellerComment(SELLER_COMMENT)
                .build();

        // then
        assertEquals(1, request.lineItems().size());
        assertEquals(LINE_ITEM_ID, request.lineItems().get(0).lineItemId());
        assertEquals(AMOUNT, request.lineItems().get(0).amount().amount());
        assertEquals(1, request.deposits().size());
        assertEquals(DEPOSIT_AMOUNT, request.deposits().get(0).totalValue().amount());
        assertEquals(1, request.surcharges().size());
        assertEquals(SURCHARGE_ID, request.surcharges().get(0).surchargeId());
        assertEquals(DELIVERY_AMOUNT, request.delivery().amount());
        assertEquals(OVERPAID_AMOUNT, request.overpaid().amount());
        assertEquals(SERVICES_AMOUNT, request.additionalServices().amount());
        assertEquals(SELLER_COMMENT, request.sellerComment());

        // and — toBuilder round-trips every component
        RefundRequest copy = request.toBuilder().build();
        assertEquals(1, copy.lineItems().size());
        assertEquals(AMOUNT, copy.lineItems().get(0).amount().amount());
        assertEquals(1, copy.deposits().size());
        assertEquals(1, copy.surcharges().size());
        assertEquals(DELIVERY_AMOUNT, copy.delivery().amount());
        assertEquals(OVERPAID_AMOUNT, copy.overpaid().amount());
        assertEquals(SERVICES_AMOUNT, copy.additionalServices().amount());
        assertEquals(SELLER_COMMENT, copy.sellerComment());
    }

    @Test
    void refundRequest_whenFullRefund_hasEmptyComponentsAndNullValues() {
        // when — only the required fields
        RefundRequest request = RefundRequest.builder()
                .paymentId(PAYMENT_ID).orderId(ORDER_ID)
                .commandId(COMMAND_ID).reason(RefundReason.REFUND)
                .build();

        // then — nothing partial is set
        assertTrue(request.lineItems().isEmpty());
        assertTrue(request.deposits().isEmpty());
        assertTrue(request.surcharges().isEmpty());
        assertNull(request.delivery());
        assertNull(request.overpaid());
        assertNull(request.additionalServices());
        assertNull(request.sellerComment());
    }

    @Test
    void refundLineItem_whenByAmount_exposesAmountAndNoQuantity() {
        // when
        RefundLineItem item = RefundLineItem.byAmount(LINE_ITEM_ID, Money.of(AMOUNT, CURRENCY));

        // then
        assertEquals(LINE_ITEM_ID, item.lineItemId());
        assertEquals(AMOUNT, item.amount().amount());
        assertNull(item.quantity());
    }

    @Test
    void refundLineItem_whenByQuantity_exposesQuantityAndNoAmount() {
        // when
        RefundLineItem item = RefundLineItem.byQuantity(LINE_ITEM_ID, QUANTITY);

        // then
        assertEquals(QUANTITY, item.quantity());
        assertNull(item.amount());
    }

    @Test
    void refundLineItem_whenLineItemIdNotUuid_throwsIllegalArgumentNamingUuid() {
        // then
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RefundLineItem.byAmount(NOT_A_UUID, Money.of(AMOUNT, CURRENCY)));
        assertTrue(failure.getMessage().contains("UUID"), failure.getMessage());
    }

    @Test
    void refundLineItem_whenAmountNull_throwsIllegalArgumentNamingAmount() {
        // then
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RefundLineItem.byAmount(LINE_ITEM_ID, null));
        assertTrue(failure.getMessage().contains("amount"), failure.getMessage());
    }

    @Test
    void refundLineItem_whenQuantityNull_throwsIllegalArgumentNamingQuantity() {
        // then
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RefundLineItem.byQuantity(LINE_ITEM_ID, null));
        assertTrue(failure.getMessage().contains("quantity"), failure.getMessage());
    }

    @Test
    void refundDeposit_whenValid_exposesFields() {
        // when
        RefundDeposit deposit = RefundDeposit.of(LINE_ITEM_ID, Money.of(DEPOSIT_AMOUNT, CURRENCY));

        // then
        assertEquals(LINE_ITEM_ID, deposit.lineItemId());
        assertEquals(DEPOSIT_AMOUNT, deposit.totalValue().amount());
    }

    @Test
    void refundDeposit_whenTotalValueNull_throwsIllegalArgumentNamingTotalValue() {
        // then
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RefundDeposit.of(LINE_ITEM_ID, null));
        assertTrue(failure.getMessage().contains("totalValue"), failure.getMessage());
    }

    @Test
    void refundSurcharge_whenValid_exposesFields() {
        // when
        RefundSurcharge surcharge =
                RefundSurcharge.of(SURCHARGE_ID, Money.of(SURCHARGE_AMOUNT, CURRENCY));

        // then
        assertEquals(SURCHARGE_ID, surcharge.surchargeId());
        assertEquals(SURCHARGE_AMOUNT, surcharge.value().amount());
    }

    @Test
    void refundSurcharge_whenSurchargeIdNotUuid_throwsIllegalArgumentNamingUuid() {
        // then
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RefundSurcharge.of(NOT_A_UUID, Money.of(SURCHARGE_AMOUNT, CURRENCY)));
        assertTrue(failure.getMessage().contains("UUID"), failure.getMessage());
    }

    @Test
    void refundReason_whenMapped_producesMatchingRawValueForEveryConstant() {
        // then — every domain reason has a Layer-1 mapping (exhaustive switch)
        for (RefundReason reason : RefundReason.values()) {
            assertEquals(reason.name(), reason.toRaw().getValue());
        }
    }

    @Test
    void paymentOperationFilter_whenAll_isEmpty() {
        // then
        PaymentOperationFilter filter = PaymentOperationFilter.all();
        assertNull(filter.paymentId());
        assertNull(filter.participantLogin());
        assertNull(filter.occurredFrom());
        assertNull(filter.occurredTo());
        assertNull(filter.group());
        assertNull(filter.marketplaceId());
        assertNull(filter.currency());
    }

    @Test
    void paymentOperationFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        PaymentOperationFilter filter = PaymentOperationFilter.builder()
                .paymentId(PAYMENT_ID).participantLogin(LOGIN)
                .occurredFrom(FROM).occurredTo(TO)
                .group(GROUP).marketplaceId(MARKETPLACE_ID).currency(CURRENCY)
                .build();

        // then
        assertEquals(PAYMENT_ID, filter.paymentId());
        assertEquals(LOGIN, filter.participantLogin());
        assertEquals(FROM, filter.occurredFrom());
        assertEquals(TO, filter.occurredTo());
        assertEquals(GROUP, filter.group());
        assertEquals(MARKETPLACE_ID, filter.marketplaceId());
        assertEquals(CURRENCY, filter.currency());

        PaymentOperationFilter copy = filter.toBuilder().build();
        assertEquals(PAYMENT_ID, copy.paymentId());
        assertEquals(CURRENCY, copy.currency());
    }

    @Test
    void refundFilter_whenAll_isEmpty() {
        // then
        RefundFilter filter = RefundFilter.all();
        assertNull(filter.refundId());
        assertNull(filter.paymentId());
        assertNull(filter.orderId());
        assertNull(filter.occurredFrom());
        assertNull(filter.occurredTo());
        assertNull(filter.status());
    }

    @Test
    void refundFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        RefundFilter filter = RefundFilter.builder()
                .refundId(PAYMENT_ID).paymentId(PAYMENT_ID).orderId(ORDER_ID)
                .occurredFrom(FROM).occurredTo(TO).status(STATUS)
                .build();

        // then
        assertEquals(PAYMENT_ID, filter.refundId());
        assertEquals(ORDER_ID, filter.orderId());
        assertEquals(STATUS, filter.status());

        RefundFilter copy = filter.toBuilder().build();
        assertEquals(PAYMENT_ID, copy.paymentId());
        assertEquals(FROM, copy.occurredFrom());
        assertEquals(TO, copy.occurredTo());
    }
}
