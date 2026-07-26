/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductDtoGroupRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductDtoRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One entry from Allegro's compatible-products database (the {@code ID}-typed
 * source a car-parts offer picks its compatibility list from) — a specific
 * vehicle/part the item fits.
 *
 * @param id the compatible-product identifier (reuse it as a compatibility-list
 *     {@code ID} item)
 * @param text the human-readable description, or {@code null} when absent
 * @param groupId the identifier of the group this product belongs to, or
 *     {@code null} when it belongs to none
 * @param attributes descriptors that disambiguate the product (year range, engine
 *     code, …); empty when none are returned
 *
 * @since 0.2.0
 */
public record CompatibleProduct(
        String id,
        @Nullable String text,
        @Nullable String groupId,
        List<CompatibleProductAttribute> attributes) {

    /** Canonical constructor; defensively copies {@code attributes} immutable. */
    public CompatibleProduct {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    /** Map one generated Layer-1 compatible product to the public record. */
    public static CompatibleProduct from(CompatibleProductDtoRaw raw) {
        return new CompatibleProduct(
                raw.getId(), raw.getText(), groupId(raw.getGroup()), attributes(raw));
    }

    private static @Nullable String groupId(@Nullable CompatibleProductDtoGroupRaw group) {
        return group == null ? null : group.getId();
    }

    private static List<CompatibleProductAttribute> attributes(CompatibleProductDtoRaw raw) {
        if (raw.getAttributes() == null) {
            return List.of();
        }
        return raw.getAttributes().stream().map(CompatibleProductAttribute::from).toList();
    }
}
