/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CommissionResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * One commission line in a {@link FeePreview}: a one-off fee charged when the
 * offer sells (e.g. the sale commission for its category).
 *
 * @param name the human-readable commission name
 * @param type the commission type identifier
 * @param fee the fee amount, or {@code null} when the preview carries none
 *
 * @since 0.3.0
 */
public record FeeCommission(@Nullable String name, @Nullable String type, @Nullable Money fee) {

    /**
     * Map the generated commission DTO to the public record.
     *
     * @param raw the generated commission DTO
     * @return the mapped record
     */
    public static FeeCommission from(CommissionResponseRaw raw) {
        return new FeeCommission(
                raw.getName(),
                raw.getType(),
                raw.getFee() == null
                        ? null
                        : Money.of(raw.getFee().getAmount(), raw.getFee().getCurrency()));
    }
}
