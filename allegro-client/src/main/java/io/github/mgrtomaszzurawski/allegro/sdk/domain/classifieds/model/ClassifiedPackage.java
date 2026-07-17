/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageConfigRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedPackageConfigsRaw;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * A classifieds (advertisement) package a seller can attach to an offer listed
 * in a category, as returned by {@code Classifieds.availablePackages(String)}.
 *
 * @param id package identifier
 * @param name package name (for example {@code Power})
 * @param type whether this is a base or an extra package
 * @param extensions bundled extensions; never {@code null}, possibly empty
 * @param promotions promotions included in the package; never {@code null},
 *     possibly empty
 * @param publication publication terms, or {@code null} when the package does
 *     not define them
 *
 * @since 0.2.0
 */
public record ClassifiedPackage(
        String id,
        String name,
        ClassifiedPackageType type,
        List<ClassifiedExtension> extensions,
        List<ClassifiedPromotion> promotions,
        @Nullable ClassifiedPublication publication) {

    public ClassifiedPackage {
        extensions = List.copyOf(extensions);
        promotions = List.copyOf(promotions);
    }

    /** Map one generated Layer-1 package DTO to the public record. */
    public static ClassifiedPackage from(ClassifiedPackageConfigRaw raw) {
        return new ClassifiedPackage(
                raw.getId(),
                raw.getName(),
                typeFrom(raw.getType()),
                mapOrEmpty(raw.getExtensions(), ClassifiedExtension::from),
                mapOrEmpty(raw.getPromotions(), ClassifiedPromotion::from),
                raw.getPublication() == null ? null : ClassifiedPublication.from(raw.getPublication()));
    }

    /** Map the generated Layer-1 list response to public records. */
    public static List<ClassifiedPackage> listFrom(ClassifiedPackageConfigsRaw raw) {
        return mapOrEmpty(raw.getPackages(), ClassifiedPackage::from);
    }

    // The generated Layer-1 enum and the public enum share constant names by
    // construction; an unrecognised server value fails earlier, in Jackson
    // deserialization, so it never reaches this mapping.
    private static ClassifiedPackageType typeFrom(ClassifiedPackageConfigRaw.TypeEnum raw) {
        return ClassifiedPackageType.valueOf(raw.name());
    }

    private static <R, T> List<T> mapOrEmpty(@Nullable List<R> raw, Function<R, T> mapper) {
        return raw == null ? List.of() : raw.stream().map(mapper).toList();
    }
}
