/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.payments;

import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundAdditionalServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundDeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundOverpaidRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.InitializeRefundRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsSurchargeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundAdditionalServicesValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundDeliveryValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemDepositRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemDepositTotalValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundLineItemValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundOverpaidValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundPaymentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefundSurchargeValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundDeposit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundLineItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundSurcharge;
import java.util.UUID;

/**
 * Builds the generated Layer-1 request body for the refund-initiation endpoint
 * from the public {@link RefundRequest}, keeping {@link PaymentsImpl} a thin
 * verb dispatcher. With only the required fields set the whole payment is
 * refunded; the optional line-item/deposit/surcharge/delivery/overpaid/
 * additional-services components drive a partial refund.
 *
 * @since 0.5.0
 */
final class PaymentsRequestFactory {

    private PaymentsRequestFactory() {
    }

    /** Request body for {@code POST /payments/refunds}. */
    static InitializeRefundRaw initializeRefund(RefundRequest request) {
        InitializeRefundRaw raw = new InitializeRefundRaw()
                .payment(new RefundPaymentRaw().id(UUID.fromString(request.paymentId())))
                .order(new RefundOrderRaw().id(UUID.fromString(request.orderId())))
                .commandId(request.commandId())
                .reason(request.reason().toRaw())
                .sellerComment(request.sellerComment());

        for (RefundLineItem lineItem : request.lineItems()) {
            raw.addLineItemsItem(toRawLineItem(lineItem));
        }
        for (RefundDeposit deposit : request.deposits()) {
            raw.addDepositsItem(toRawDeposit(deposit));
        }
        for (RefundSurcharge surcharge : request.surcharges()) {
            raw.addSurchargesItem(toRawSurcharge(surcharge));
        }
        if (request.delivery() != null) {
            raw.delivery(new InitializeRefundDeliveryRaw()
                    .value(deliveryValue(request.delivery())));
        }
        if (request.overpaid() != null) {
            raw.overpaid(new InitializeRefundOverpaidRaw()
                    .value(overpaidValue(request.overpaid())));
        }
        if (request.additionalServices() != null) {
            raw.additionalServices(new InitializeRefundAdditionalServicesRaw()
                    .value(additionalServicesValue(request.additionalServices())));
        }
        return raw;
    }

    private static RefundLineItemRaw toRawLineItem(RefundLineItem lineItem) {
        RefundLineItemRaw raw = new RefundLineItemRaw()
                .id(UUID.fromString(lineItem.lineItemId()));
        Money amount = lineItem.amount();
        if (amount != null) {
            raw.type(RefundLineItemRaw.TypeEnum.AMOUNT)
                    .value(new RefundLineItemValueRaw()
                            .amount(amount.amount()).currency(amount.currency()));
        } else {
            raw.type(RefundLineItemRaw.TypeEnum.QUANTITY).quantity(lineItem.quantity());
        }
        return raw;
    }

    private static RefundLineItemDepositRaw toRawDeposit(RefundDeposit deposit) {
        Money totalValue = deposit.totalValue();
        return new RefundLineItemDepositRaw()
                .lineItemId(UUID.fromString(deposit.lineItemId()))
                .totalValue(new RefundLineItemDepositTotalValueRaw()
                        .amount(totalValue.amount()).currency(totalValue.currency()));
    }

    private static PaymentsSurchargeRaw toRawSurcharge(RefundSurcharge surcharge) {
        Money value = surcharge.value();
        return new PaymentsSurchargeRaw()
                .id(UUID.fromString(surcharge.surchargeId()))
                .value(new RefundSurchargeValueRaw()
                        .amount(value.amount()).currency(value.currency()));
    }

    private static RefundDeliveryValueRaw deliveryValue(Money money) {
        return new RefundDeliveryValueRaw().amount(money.amount()).currency(money.currency());
    }

    private static RefundOverpaidValueRaw overpaidValue(Money money) {
        return new RefundOverpaidValueRaw().amount(money.amount()).currency(money.currency());
    }

    private static RefundAdditionalServicesValueRaw additionalServicesValue(Money money) {
        return new RefundAdditionalServicesValueRaw()
                .amount(money.amount()).currency(money.currency());
    }
}
