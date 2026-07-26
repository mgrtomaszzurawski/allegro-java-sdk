/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The classifieds packages an offer is listed with, previewed for their fees: a
 * base package plus any extra packages. Applies only to classifieds
 * (advertisement) categories.
 *
 * @param basePackageId the base package id, or {@code null} to preview extras
 *     against the offer's current base package
 * @param extraPackages the additional packages, empty when none
 *
 * @since 0.1.0
 */
public record ClassifiedsPackages(
        @Nullable String basePackageId,
        List<ClassifiedsExtraPackage> extraPackages) {

    /** Compact constructor taking a defensive copy of the extra packages. */
    public ClassifiedsPackages {
        extraPackages = List.copyOf(extraPackages);
    }

    /**
     * Only a base package, no extras.
     *
     * @param basePackageId the base package id
     * @return the classifieds packages
     */
    public static ClassifiedsPackages ofBasePackage(String basePackageId) {
        return new ClassifiedsPackages(basePackageId, List.of());
    }
}
