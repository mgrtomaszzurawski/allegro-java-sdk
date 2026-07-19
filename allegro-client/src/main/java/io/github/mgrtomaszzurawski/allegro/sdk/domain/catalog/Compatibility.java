/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CompatibleCategory;
import java.util.List;

/**
 * Vehicle/part compatibility lists — reached via {@code catalog().compatibility()}.
 *
 * <p>Some categories (car parts and accessories) let an offer carry a
 * compatibility list: the set of vehicles or products the item fits. This
 * sub-facade reads Allegro's reference data for building such lists.
 *
 * @since 0.2.0
 */
public interface Compatibility {

    /**
     * Lists the categories in which a compatibility list is supported, each with
     * how its items are supplied and the bounds on a free-text list.
     *
     * @return the supported categories; empty when none are returned
     */
    List<CompatibleCategory> supportedCategories();
}
