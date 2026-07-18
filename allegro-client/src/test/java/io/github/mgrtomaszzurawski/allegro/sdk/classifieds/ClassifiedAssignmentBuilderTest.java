/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.classifieds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedExtraPackage;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip coverage of {@link ClassifiedAssignment.builder()}: the required
 * base package, optional extras with and without a republish flag, the
 * {@code toBuilder} copy, and the fail-fast on the missing required field.
 */
class ClassifiedAssignmentBuilderTest {

    private static final String BASE_PACKAGE_ID = "6174be19-56f9-484b-b72c-43b0b00785e8";
    private static final String EXTRA_PACKAGE_ID = "3b2f0c11-0000-4a5e-a55c-bcb8e7d53cbb";
    private static final String OTHER_EXTRA_ID = "9c1d2e3f-1111-4b6a-8c7d-0a1b2c3d4e5f";
    private static final String BLANK = "   ";

    @Test
    void build_whenOnlyBasePackage_hasNoExtras() {
        // when
        ClassifiedAssignment assignment = ClassifiedAssignment.builder()
                .basePackage(BASE_PACKAGE_ID)
                .build();

        // then
        assertEquals(BASE_PACKAGE_ID, assignment.basePackageId());
        assertTrue(assignment.extraPackages().isEmpty());
    }

    @Test
    void build_whenExtrasAdded_keepsIdsAndRepublishFlags() {
        // when — one extra left to the server default, one with an explicit flag
        ClassifiedAssignment assignment = ClassifiedAssignment.builder()
                .basePackage(BASE_PACKAGE_ID)
                .addExtraPackage(EXTRA_PACKAGE_ID)
                .addExtraPackage(OTHER_EXTRA_ID, true)
                .build();

        // then
        assertEquals(2, assignment.extraPackages().size());
        assertEquals(EXTRA_PACKAGE_ID, assignment.extraPackages().get(0).id());
        assertNull(assignment.extraPackages().get(0).republish());
        assertEquals(OTHER_EXTRA_ID, assignment.extraPackages().get(1).id());
        assertEquals(Boolean.TRUE, assignment.extraPackages().get(1).republish());
    }

    @Test
    void extraPackages_whenListSet_replacesPreviousExtras() {
        // when — a previously added extra is discarded by the list setter
        ClassifiedAssignment assignment = ClassifiedAssignment.builder()
                .basePackage(BASE_PACKAGE_ID)
                .addExtraPackage(EXTRA_PACKAGE_ID)
                .extraPackages(List.of(new ClassifiedExtraPackage(OTHER_EXTRA_ID, false)))
                .build();

        // then
        assertEquals(1, assignment.extraPackages().size());
        assertEquals(OTHER_EXTRA_ID, assignment.extraPackages().get(0).id());
        assertEquals(Boolean.FALSE, assignment.extraPackages().get(0).republish());
    }

    @Test
    void toBuilder_whenRebuilt_preservesBaseAndExtras() {
        // given
        ClassifiedAssignment original = ClassifiedAssignment.builder()
                .basePackage(BASE_PACKAGE_ID)
                .addExtraPackage(EXTRA_PACKAGE_ID, true)
                .build();

        // when
        ClassifiedAssignment copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenBasePackageMissing_throwsIllegalState() {
        var builder = ClassifiedAssignment.builder().addExtraPackage(EXTRA_PACKAGE_ID);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenBasePackageBlank_throwsIllegalState() {
        var builder = ClassifiedAssignment.builder().basePackage(BLANK);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }
}
