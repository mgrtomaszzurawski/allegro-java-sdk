/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder.ClassifiedAssignmentBuilder;
import java.util.List;

/**
 * The set of classifieds packages to assign to an offer — one required base
 * package and any number of optional extra packages. Build it with
 * {@link #builder()}, which validates the required base package fail-fast, and
 * pass it to {@code Classifieds.assignPackages(String, ClassifiedAssignment)}.
 *
 * @param basePackageId identifier of the base package to assign (required)
 * @param extraPackages the extra packages to assign; never {@code null},
 *     possibly empty
 *
 * @since 0.2.0
 */
public record ClassifiedAssignment(String basePackageId, List<ClassifiedExtraPackage> extraPackages) {

    public ClassifiedAssignment {
        extraPackages = List.copyOf(extraPackages);
    }

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link ClassifiedAssignmentBuilder}
     */
    public static ClassifiedAssignmentBuilder builder() {
        return new ClassifiedAssignmentBuilder();
    }

    /**
     * A builder pre-populated with this assignment's fields, for deriving a
     * modified copy.
     *
     * @return a builder holding this assignment's values
     */
    public ClassifiedAssignmentBuilder toBuilder() {
        return new ClassifiedAssignmentBuilder()
                .basePackage(basePackageId)
                .extraPackages(extraPackages);
    }
}
