/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseOffersInnerCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseOffersInnerParametersInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseOffersInnerRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One of the seller's offers together with the category parameters it is still
 * missing — reached from {@code offers().streamUnfilledParameters()}. Filling
 * these improves the offer's completeness and searchability.
 *
 * @param offerId      the offer identifier
 * @param categoryId   the offer's category, or {@code null} when absent
 * @param parameterIds ids of the category parameters the offer has not filled in
 * @since 0.2.0
 */
public record UnfilledParameters(
        String offerId,
        @Nullable String categoryId,
        List<String> parameterIds) {

    public UnfilledParameters {
        parameterIds = List.copyOf(parameterIds);
    }

    /** Project a generated unfilled-parameters entry onto the consumer record. */
    public static UnfilledParameters from(UnfilledParametersResponseOffersInnerRaw raw) {
        UnfilledParametersResponseOffersInnerCategoryRaw category = raw.getCategory();
        List<UnfilledParametersResponseOffersInnerParametersInnerRaw> parameters = raw.getParameters();
        List<String> parameterIds = parameters == null
                ? List.of()
                : parameters.stream()
                        .map(UnfilledParametersResponseOffersInnerParametersInnerRaw::getId)
                        .filter(Objects::nonNull)
                        .toList();
        return new UnfilledParameters(
                raw.getId(),
                category == null ? null : category.getId(),
                parameterIds);
    }
}
