/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.FeePreviewResponseRaw;
import java.util.List;

/**
 * The fees a draft offer would incur, as returned by
 * {@code pricing().feePreview(...)}: the one-off {@link FeeCommission commissions}
 * charged on sale and the recurring {@link FeeQuote quotes} charged while listed.
 *
 * @param commissions the one-off commission lines (possibly empty)
 * @param quotes the recurring quote lines (possibly empty)
 *
 * @since 0.3.0
 */
public record FeePreview(List<FeeCommission> commissions, List<FeeQuote> quotes) {

    /** Defensively copies both fee lists so the record stays immutable. */
    public FeePreview {
        commissions = List.copyOf(commissions);
        quotes = List.copyOf(quotes);
    }

    /**
     * Map the generated response DTO to the public record.
     *
     * @param raw the generated fee-preview DTO
     * @return the mapped record
     */
    public static FeePreview from(FeePreviewResponseRaw raw) {
        return new FeePreview(
                raw.getCommissions() == null
                        ? List.of()
                        : raw.getCommissions().stream().map(FeeCommission::from).toList(),
                raw.getQuotes() == null
                        ? List.of()
                        : raw.getQuotes().stream().map(FeeQuote::from).toList());
    }
}
