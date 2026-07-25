/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemAdditionalInfoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListTextItemRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One entry of an offer's "fits to" ({@code compatibilityList}) section — the compatibility of the
 * product being sold with a specific model (typically a vehicle from the TecDoc database). An entry
 * is one of two mutually exclusive kinds, each created through its own factory:
 *
 * <ul>
 *   <li>{@link #text(String)} — a free-text compatibility line
 *       (e.g. {@code "CITROËN C6 (TD_) 2005/09-2011/12 2.7 HDi 204KM/150kW"});</li>
 *   <li>{@link #productId(String)} / {@link #productId(String, List)} — a compatible-product
 *       identifier obtained from the {@code offers().compatibility()} lookup, optionally narrowed by
 *       additional-info values (the extra attribute values that disambiguate the product).</li>
 * </ul>
 *
 * <p>The section is only accepted in categories that support "fits to"; check with
 * {@code offers().compatibility().supportedCategories(...)} before setting it.
 *
 * @since 0.4.0
 */
public record CompatibilityEntry(
        Kind kind,
        @Nullable String text,
        @Nullable String productId,
        List<String> additionalInfo) {

    /** Which of the two compatibility-entry forms this is. */
    public enum Kind {

        /** A free-text compatibility line carried in {@link CompatibilityEntry#text()}. */
        TEXT,

        /** A compatible-product identifier carried in {@link CompatibilityEntry#productId()}. */
        PRODUCT_ID
    }

    /** Canonical constructor: defends the invariants and defensively copies {@code additionalInfo}. */
    public CompatibilityEntry {
        Objects.requireNonNull(kind, "kind");
        additionalInfo = additionalInfo == null ? List.of() : List.copyOf(additionalInfo);
    }

    /** A free-text compatibility line (non-null). */
    public static CompatibilityEntry text(String text) {
        return new CompatibilityEntry(Kind.TEXT, Objects.requireNonNull(text, "text"), null, List.of());
    }

    /** A compatible-product identifier (non-null), with no additional-info narrowing. */
    public static CompatibilityEntry productId(String productId) {
        return productId(productId, List.of());
    }

    /** A compatible-product identifier (non-null) narrowed by the given additional-info values. */
    public static CompatibilityEntry productId(String productId, List<String> additionalInfo) {
        return new CompatibilityEntry(
                Kind.PRODUCT_ID, null, Objects.requireNonNull(productId, "productId"), additionalInfo);
    }

    /**
     * The generated union item for this entry: a text item for {@link Kind#TEXT}, otherwise an id item
     * carrying the product identifier and any additional-info values.
     */
    public CompatibilityListItemRaw toRaw() {
        if (kind == Kind.TEXT) {
            return new CompatibilityListItemRaw(new CompatibilityListTextItemRaw()
                    .type(CompatibilityListTextItemRaw.TypeEnum.TEXT)
                    .text(text));
        }
        CompatibilityListIdItemRaw idItem = new CompatibilityListIdItemRaw()
                .type(CompatibilityListIdItemRaw.TypeEnum.ID)
                .id(productId);
        if (!additionalInfo.isEmpty()) {
            idItem.additionalInfo(additionalInfo.stream()
                    .map(value -> new CompatibilityListIdItemAdditionalInfoRaw().value(value))
                    .toList());
        }
        return new CompatibilityListItemRaw(idItem);
    }
}
