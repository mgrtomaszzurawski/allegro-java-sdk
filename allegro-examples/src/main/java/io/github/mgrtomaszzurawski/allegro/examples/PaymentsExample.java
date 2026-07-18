/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentRefund;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.RefundReason;
import java.util.UUID;

/**
 * Compile-only twin of the {@code docs/payments.md} refund snippet — if the
 * documented consumer code stops compiling, this module breaks the build.
 */
public final class PaymentsExample {

    private PaymentsExample() {
    }

    static PaymentRefund refundPayment(String clientId, String clientSecret,
            String paymentId, String orderId) {
        var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
                auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));

        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            PaymentRefund refund = client.payments().refund(RefundRequest.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .commandId(UUID.randomUUID().toString())
                    .reason(RefundReason.COMPLAINT)
                    .build());
            System.out.println("Refund " + refund.id() + " is " + refund.status());
            return refund;
        }
    }
}
