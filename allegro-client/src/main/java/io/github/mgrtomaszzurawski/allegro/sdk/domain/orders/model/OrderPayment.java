/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CheckoutFormPaymentReferenceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A payment on an order — the main payment ({@link Order#payment()}) or one of
 * the order's surcharges ({@link Order#surcharges()}), which share this shape.
 *
 * <p>The {@code id} and {@code type} are always present (the spec marks them
 * required); the remaining fields are optional because a not-yet-completed payment
 * may carry no provider, completion time, or amount yet.
 *
 * @param id payment identifier
 * @param type how the order was paid
 * @param provider which provider processed the payment, or {@code null} when not set
 * @param finishedAt when the payment completed, or {@code null} when not finished
 * @param paidAmount the amount actually paid, or {@code null} when not set
 * @param reconciliation the internal reconciliation amount, or {@code null} when not set
 * @param features payment feature flags (e.g. split-payment markers); never
 *     {@code null}, possibly empty
 *
 * @since 0.7.0
 */
public record OrderPayment(
        String id,
        PaymentType type,
        @Nullable PaymentProvider provider,
        @Nullable OffsetDateTime finishedAt,
        @Nullable Money paidAmount,
        @Nullable Money reconciliation,
        List<String> features) {

    public OrderPayment {
        features = features == null ? List.of() : List.copyOf(features);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static OrderPayment from(CheckoutFormPaymentReferenceRaw raw) {
        return new OrderPayment(
                raw.getId().toString(),
                PaymentType.from(raw.getType()),
                raw.getProvider() == null ? null : PaymentProvider.from(raw.getProvider()),
                raw.getFinishedAt(),
                Prices.money(raw.getPaidAmount()),
                Prices.money(raw.getReconciliation()),
                raw.getFeatures());
    }
}
