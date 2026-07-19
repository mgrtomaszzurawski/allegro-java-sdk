/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StandardizedDescriptionRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An offer's standardized description — an ordered list of {@link DescriptionSection}s,
 * each a group of text blocks and images.
 *
 * <p>The same immutable value is used both ways: build one to set an offer's
 * description on {@code CreateOfferRequest}, or read one back from an {@link Offer}.
 *
 * <pre>{@code
 * OfferDescription description = OfferDescription.of(
 *         DescriptionSection.of(DescriptionItem.text("<h1>Mechanical keyboard</h1>")),
 *         DescriptionSection.of(DescriptionItem.image("https://img.example/keyboard.jpg")));
 * }</pre>
 *
 * @param sections the description's sections, in display order
 * @since 0.3.0
 */
public record OfferDescription(List<DescriptionSection> sections) {

    /** Canonical constructor; defensively copies the sections into an immutable list. */
    public OfferDescription {
        sections = List.copyOf(sections);
    }

    /** A description of the given sections, in order. */
    public static OfferDescription of(DescriptionSection... sections) {
        return new OfferDescription(List.of(sections));
    }

    /**
     * Project a generated description onto the consumer value.
     *
     * @param raw the generated description (may be {@code null})
     * @return the mapped value, or {@code null} if {@code raw} is {@code null}
     */
    public static @Nullable OfferDescription from(@Nullable StandardizedDescriptionRaw raw) {
        if (raw == null) {
            return null;
        }
        List<DescriptionSectionRaw> rawSections = raw.getSections();
        return new OfferDescription(rawSections == null
                ? List.of()
                : rawSections.stream().map(DescriptionSection::from).toList());
    }

    /** The generated description for this value. */
    public StandardizedDescriptionRaw toRaw() {
        return new StandardizedDescriptionRaw().sections(sections.stream().map(DescriptionSection::toRaw).toList());
    }
}
