/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedExtraPackageRaw;
import org.jspecify.annotations.Nullable;

/**
 * An extra (add-on) classifieds package layered on top of an offer's base
 * package — both when reading the packages assigned to an offer
 * ({@code Classifieds.packagesOfOffer(String)}) and when assigning them
 * ({@code Classifieds.assignPackages(String, ClassifiedAssignment)}).
 *
 * @param id the extra package identifier
 * @param republish whether reassigning the package republishes the
 *     advertisement, or {@code null} when it is left to the server default
 *
 * @since 0.2.0
 */
public record ClassifiedExtraPackage(String id, @Nullable Boolean republish) {

    /** Map the generated Layer-1 DTO to the public record. */
    static ClassifiedExtraPackage from(ClassifiedExtraPackageRaw raw) {
        return new ClassifiedExtraPackage(raw.getId(), raw.getRepublish());
    }
}
