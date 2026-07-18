/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BaseOperationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OperationValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One entry in the seller's payment-operations history: a dated money movement on
 * the payment wallet (an income, outcome, refund or blockade).
 *
 * @param type the operation type (raw Allegro value, e.g. {@code CHARGE})
 * @param group the wallet group, or {@code null}: {@code INCOME}, {@code OUTCOME},
 *     {@code REFUND} or {@code BLOCKADES}
 * @param value the signed amount of the operation, or {@code null} when absent
 * @param occurredAt when the operation happened, or {@code null}
 * @param marketplaceId the marketplace the operation belongs to, or {@code null}
 *
 * @since 0.5.0
 */
public record PaymentOperation(
        String type,
        @Nullable String group,
        @Nullable Money value,
        @Nullable OffsetDateTime occurredAt,
        @Nullable String marketplaceId) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static PaymentOperation from(BaseOperationRaw raw) {
        var group = raw.getGroup();
        OperationValueRaw value = raw.getValue();
        return new PaymentOperation(
                raw.getType(),
                group == null ? null : group.getValue(),
                value == null ? null : Money.of(value.getAmount(), value.getCurrency()),
                raw.getOccurredAt(),
                raw.getMarketplaceId());
    }
}
