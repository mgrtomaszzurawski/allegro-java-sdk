/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.classifieds;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedExtraPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackagesRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedExtraPackage;

/**
 * Maps the public {@link ClassifiedAssignment} request to its generated Layer-1
 * request body. Response mapping lives in the domain records' {@code from}
 * factories; only the write direction needs a mapper.
 */
final class ClassifiedsMapper {

    private ClassifiedsMapper() {
    }

    /** Build the {@code PUT} body assigning the base and extra packages. */
    static ClassifiedPackagesRaw toRaw(ClassifiedAssignment assignment) {
        ClassifiedPackagesRaw raw = new ClassifiedPackagesRaw()
                .basePackage(new ClassifiedPackageRaw().id(assignment.basePackageId()));
        for (ClassifiedExtraPackage extra : assignment.extraPackages()) {
            raw.addExtraPackagesItem(new ClassifiedExtraPackageRaw()
                    .id(extra.id())
                    .republish(extra.republish()));
        }
        return raw;
    }
}
