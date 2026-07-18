/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.StandardizedDescriptionRaw;
import java.util.List;

/**
 * A standardized offer description — the ordered list of {@link DescriptionSection
 * sections} Allegro uses for an offer's rich description. Used both as the
 * description translation returned by {@code OfferTranslations.ofOffer(String)}
 * and as the translated description supplied to {@code
 * OfferTranslations.update(...)}.
 *
 * @param sections the description sections in order; never {@code null}, possibly
 *     empty
 *
 * @since 0.2.0
 */
public record StandardizedDescription(List<DescriptionSection> sections) {

    public StandardizedDescription {
        sections = List.copyOf(sections);
    }

    /**
     * A description made of the given sections.
     *
     * @param sections the description sections in order
     * @return a standardized description
     */
    public static StandardizedDescription of(DescriptionSection... sections) {
        return new StandardizedDescription(List.of(sections));
    }

    /** Map the generated Layer-1 description to the public record. */
    static StandardizedDescription from(StandardizedDescriptionRaw raw) {
        return new StandardizedDescription(raw.getSections() == null
                ? List.of()
                : raw.getSections().stream().map(DescriptionSection::from).toList());
    }
}
