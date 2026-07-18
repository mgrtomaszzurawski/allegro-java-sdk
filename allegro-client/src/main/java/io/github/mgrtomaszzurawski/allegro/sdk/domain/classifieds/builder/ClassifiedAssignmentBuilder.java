/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedExtraPackage;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link ClassifiedAssignment}. The base package is required;
 * extra packages are optional and may be added one at a time or set as a whole.
 * {@link #build()} validates the required base package fail-fast.
 *
 * @since 0.2.0
 */
public final class ClassifiedAssignmentBuilder {

    private static final String ERR_BASE_PACKAGE_REQUIRED = "basePackageId is required";

    private @Nullable String basePackageId;
    private final List<ClassifiedExtraPackage> extraPackages = new ArrayList<>();

    /**
     * Set the base package to assign (required).
     *
     * @param packageId the base package identifier
     * @return this builder
     */
    public ClassifiedAssignmentBuilder basePackage(String packageId) {
        this.basePackageId = packageId;
        return this;
    }

    /**
     * Add an extra package, leaving the republish flag to the server default.
     *
     * @param packageId the extra package identifier
     * @return this builder
     */
    public ClassifiedAssignmentBuilder addExtraPackage(String packageId) {
        this.extraPackages.add(new ClassifiedExtraPackage(packageId, null));
        return this;
    }

    /**
     * Add an extra package with an explicit republish flag.
     *
     * @param packageId the extra package identifier
     * @param republish whether reassigning the package republishes the
     *     advertisement
     * @return this builder
     */
    public ClassifiedAssignmentBuilder addExtraPackage(String packageId, boolean republish) {
        this.extraPackages.add(new ClassifiedExtraPackage(packageId, republish));
        return this;
    }

    /**
     * Replace the extra packages with the given list.
     *
     * @param packages the extra packages to assign
     * @return this builder
     */
    public ClassifiedAssignmentBuilder extraPackages(List<ClassifiedExtraPackage> packages) {
        this.extraPackages.clear();
        this.extraPackages.addAll(packages);
        return this;
    }

    /**
     * Validate and build the assignment.
     *
     * @return the immutable assignment
     * @throws IllegalStateException if the base package is missing
     */
    public ClassifiedAssignment build() {
        if (basePackageId == null || basePackageId.isBlank()) {
            throw new IllegalStateException(ERR_BASE_PACKAGE_REQUIRED);
        }
        return new ClassifiedAssignment(basePackageId, List.copyOf(extraPackages));
    }
}
