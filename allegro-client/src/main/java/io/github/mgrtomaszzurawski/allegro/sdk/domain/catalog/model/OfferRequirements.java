/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferRequirementsRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The offer-level requirements a {@link Product} imposes on offers built from it:
 * a requirements id and the parameter values an offer must carry.
 *
 * @param id the offer-requirements id, or {@code null}
 * @param parameters the required parameter values, in order; never {@code null},
 *     possibly empty
 *
 * @since 0.4.0
 */
public record OfferRequirements(
        @Nullable String id,
        List<ProductParameterValue> parameters) {

    /** Canonical constructor — defensively copies the parameters. */
    public OfferRequirements {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable OfferRequirements from(@Nullable OfferRequirementsRaw raw) {
        if (raw == null) {
            return null;
        }
        List<ProductParameterValue> parameters = raw.getParameters() == null ? List.of()
                : raw.getParameters().stream().map(ProductParameterValue::from).toList();
        return new OfferRequirements(raw.getId(), parameters);
    }
}
