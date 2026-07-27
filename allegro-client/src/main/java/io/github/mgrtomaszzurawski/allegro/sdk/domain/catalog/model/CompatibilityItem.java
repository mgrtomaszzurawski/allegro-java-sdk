/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemAdditionalInfoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListItemProductBasedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListTextItemRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One entry of a {@link CompatibilityList}: a single vehicle or part the offer is
 * compatible with.
 *
 * <p>An {@link CompatibilityInputType#ID ID} item is one picked from Allegro's
 * compatible-products database — it carries the {@link #id()} to reuse when
 * building the offer's own list, an optional human {@link #text()} label, and any
 * {@link #additionalInfo()} that disambiguates it (an engine code, an equipment
 * version). A {@link CompatibilityInputType#TEXT TEXT} item is a free-text /
 * informational entry with only its {@link #text()} set.
 *
 * @param type whether the item is identified by id or is free text
 * @param id the compatible-product id for an {@code ID} item, otherwise {@code null}
 * @param text the human-readable label, or {@code null} when absent
 * @param additionalInfo extra descriptors for an {@code ID} item (engine code,
 *     equipment version); empty for a {@code TEXT} item
 *
 * @since 0.2.0
 */
public record CompatibilityItem(
        CompatibilityInputType type,
        @Nullable String id,
        @Nullable String text,
        List<String> additionalInfo) {

    /** Canonical constructor; defensively copies {@code additionalInfo} immutable. */
    public CompatibilityItem {
        additionalInfo = additionalInfo == null ? List.of() : List.copyOf(additionalInfo);
    }

    /** Map a generated Layer-1 id-typed manual-list item to the public record. */
    public static CompatibilityItem fromIdItem(CompatibilityListIdItemRaw raw) {
        return new CompatibilityItem(
                CompatibilityInputType.ID, raw.getId(), raw.getText(),
                additionalInfoValues(raw.getAdditionalInfo()));
    }

    /** Map a generated Layer-1 text-typed manual-list item to the public record. */
    public static CompatibilityItem fromTextItem(CompatibilityListTextItemRaw raw) {
        return new CompatibilityItem(CompatibilityInputType.TEXT, null, raw.getText(), List.of());
    }

    /**
     * Map a generated Layer-1 product-based item to the public record. Such items
     * are an informational text representation only, so they map to a
     * {@link CompatibilityInputType#TEXT TEXT} entry carrying just the text.
     */
    public static CompatibilityItem fromProductBased(CompatibilityListItemProductBasedRaw raw) {
        return new CompatibilityItem(CompatibilityInputType.TEXT, null, raw.getText(), List.of());
    }

    /**
     * The forward-compat landing for an item variant this SDK version does not
     * model (its {@code type} discriminator is neither {@code ID} nor {@code TEXT}):
     * {@link CompatibilityInputType#UNKNOWN} with no id, text or extra info.
     */
    public static CompatibilityItem unknownItem() {
        return new CompatibilityItem(CompatibilityInputType.UNKNOWN, null, null, List.of());
    }

    private static List<String> additionalInfoValues(
            @Nullable List<CompatibilityListIdItemAdditionalInfoRaw> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(CompatibilityListIdItemAdditionalInfoRaw::getValue).toList();
    }
}
