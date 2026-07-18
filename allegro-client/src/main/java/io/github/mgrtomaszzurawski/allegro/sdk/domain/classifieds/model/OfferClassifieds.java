/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedResponseRaw;
import java.util.List;

/**
 * The classifieds packages currently assigned to an offer, as returned by
 * {@code Classifieds.packagesOfOffer(String)}: exactly one base package plus any
 * number of extra packages.
 *
 * @param basePackageId identifier of the assigned base package
 * @param extraPackages the assigned extra packages; never {@code null}, possibly
 *     empty
 *
 * @since 0.2.0
 */
public record OfferClassifieds(String basePackageId, List<ClassifiedExtraPackage> extraPackages) {

    public OfferClassifieds {
        extraPackages = List.copyOf(extraPackages);
    }

    /** Map the generated Layer-1 response to the public record. */
    public static OfferClassifieds from(ClassifiedResponseRaw raw) {
        return new OfferClassifieds(
                raw.getBasePackage().getId(),
                raw.getExtraPackages().stream().map(ClassifiedExtraPackage::from).toList());
    }
}
