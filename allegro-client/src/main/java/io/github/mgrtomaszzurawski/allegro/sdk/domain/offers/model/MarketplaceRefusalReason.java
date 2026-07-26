/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesRefusalReasonResponseRaw;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A reason an offer's publication was refused on an additional marketplace.
 *
 * @param code        the machine-readable refusal code, or {@code null}
 * @param userMessage a human-readable explanation, or {@code null}
 * @param parameters  extra per-code detail (parameter name to values), empty when none
 * @since 0.6.0
 */
public record MarketplaceRefusalReason(
        @Nullable String code,
        @Nullable String userMessage,
        Map<String, List<String>> parameters) {

    /** Canonical constructor: normalizes {@code parameters} to an immutable copy. */
    public MarketplaceRefusalReason {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    /** Project a generated refusal reason onto the consumer value. */
    public static MarketplaceRefusalReason from(AdditionalMarketplacesRefusalReasonResponseRaw raw) {
        return new MarketplaceRefusalReason(raw.getCode(), raw.getUserMessage(), raw.getParameters());
    }
}
