/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CaptionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SizeTableTemplateImageResponseRaw;
import java.util.List;

/**
 * The illustration attached to a size-table template: an image {@code url} and
 * the {@code captions} that annotate where each measurement is taken.
 *
 * @param url the image URL
 * @param captions the image captions
 *
 * @since 0.3.0
 */
public record SizeTableTemplateImage(String url, List<SizeTableCaption> captions) {

    /** Canonical constructor — defensively copies the captions. */
    public SizeTableTemplateImage {
        captions = captions == null ? List.of() : List.copyOf(captions);
    }

    /** Map the generated Layer-1 DTO. */
    public static SizeTableTemplateImage from(SizeTableTemplateImageResponseRaw raw) {
        List<CaptionRaw> rawCaptions = raw.getCaptions() == null ? List.of() : raw.getCaptions();
        List<SizeTableCaption> captions = rawCaptions.stream().map(SizeTableCaption::from).toList();
        return new SizeTableTemplateImage(raw.getUrl(), captions);
    }
}
