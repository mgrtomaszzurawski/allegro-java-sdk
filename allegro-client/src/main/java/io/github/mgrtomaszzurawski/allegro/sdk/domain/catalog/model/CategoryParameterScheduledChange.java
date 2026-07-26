/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParametersScheduledBaseChangeCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParametersScheduledBaseChangeParameterRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryParametersScheduledBaseChangeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RequirementChangeRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A planned change to a category's parameters — announced ahead of time so sellers
 * can prepare. Today the only kind is a {@link ScheduledChangeType#REQUIREMENT_CHANGE}:
 * a parameter's requirement (whether it must be filled) will change.
 *
 * <p>A type Allegro adds after this SDK release reads back as
 * {@link ScheduledChangeType#UNKNOWN} with a {@code null} {@link #categoryId()} and
 * {@link #parameterId()} rather than failing the read.
 *
 * @param type the change kind
 * @param scheduledAt when the change was announced
 * @param scheduledFor when the change takes effect
 * @param categoryId the affected category id, or {@code null} for an unrecognised type
 * @param parameterId the affected parameter id, or {@code null} for an unrecognised type
 *
 * @since 0.2.0
 */
public record CategoryParameterScheduledChange(
        ScheduledChangeType type,
        OffsetDateTime scheduledAt,
        OffsetDateTime scheduledFor,
        @Nullable String categoryId,
        @Nullable String parameterId) {

    /** Project a generated (polymorphic) scheduled change onto the consumer record. */
    public static CategoryParameterScheduledChange from(CategoryParametersScheduledBaseChangeRaw raw) {
        return new CategoryParameterScheduledChange(
                ScheduledChangeType.from(raw.getType()), raw.getScheduledAt(), raw.getScheduledFor(),
                categoryId(raw), parameterId(raw));
    }

    private static @Nullable String categoryId(CategoryParametersScheduledBaseChangeRaw raw) {
        // The affected category/parameter live on the concrete subtype; an unrecognised
        // type (deserialized to the base, forward-compat) yields null.
        if (raw instanceof RequirementChangeRaw change) {
            CategoryParametersScheduledBaseChangeCategoryRaw category = change.getCategory();
            return category == null ? null : category.getId();
        }
        return null;
    }

    private static @Nullable String parameterId(CategoryParametersScheduledBaseChangeRaw raw) {
        if (raw instanceof RequirementChangeRaw change) {
            CategoryParametersScheduledBaseChangeParameterRaw parameter = change.getParameter();
            return parameter == null ? null : parameter.getId();
        }
        return null;
    }
}
