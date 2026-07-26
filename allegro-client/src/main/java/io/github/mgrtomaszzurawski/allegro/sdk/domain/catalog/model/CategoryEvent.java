/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryBaseEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryCreatedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDeletedEventAllOfRedirectCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDeletedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryEventBaseCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryMovedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryRenamedEventRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One change in Allegro's category tree — a category was created, deleted, moved to
 * a different parent, or renamed.
 *
 * <p>The {@link #id() id} doubles as the cursor to resume a stream from. A type
 * Allegro adds after this SDK release reads back as {@link CategoryEventType#UNKNOWN}
 * with a {@code null} {@link #category()} rather than failing the stream.
 *
 * @param id the event id (also the cursor to resume a stream from)
 * @param type the change kind
 * @param occurredAt when the change happened
 * @param category the affected category as it stood at the event, or {@code null}
 *     for an unrecognised event type
 * @param redirectCategoryId for a {@link CategoryEventType#CATEGORY_DELETED} event,
 *     the category its offers redirect to; otherwise {@code null}
 *
 * @since 0.2.0
 */
public record CategoryEvent(
        String id,
        CategoryEventType type,
        OffsetDateTime occurredAt,
        @Nullable CategoryEventCategory category,
        @Nullable String redirectCategoryId) {

    /** Project a generated (polymorphic) category event onto the consumer record. */
    public static CategoryEvent from(CategoryBaseEventRaw raw) {
        return new CategoryEvent(
                raw.getId(), CategoryEventType.from(raw.getType()), raw.getOccurredAt(),
                categoryOf(raw), redirectCategoryId(raw));
    }

    private static @Nullable CategoryEventCategory categoryOf(CategoryBaseEventRaw raw) {
        // Each generated subtype carries the affected category under its own type; an
        // unrecognised type (deserialized to the base, forward-compat) yields null.
        CategoryEventBaseCategoryRaw category = null;
        if (raw instanceof CategoryCreatedEventRaw event) {
            category = event.getCategory();
        } else if (raw instanceof CategoryDeletedEventRaw event) {
            category = event.getCategory();
        } else if (raw instanceof CategoryMovedEventRaw event) {
            category = event.getCategory();
        } else if (raw instanceof CategoryRenamedEventRaw event) {
            category = event.getCategory();
        }
        return category == null ? null : CategoryEventCategory.from(category);
    }

    private static @Nullable String redirectCategoryId(CategoryBaseEventRaw raw) {
        if (raw instanceof CategoryDeletedEventRaw event) {
            CategoryDeletedEventAllOfRedirectCategoryRaw redirect = event.getRedirectCategory();
            return redirect == null ? null : redirect.getId();
        }
        return null;
    }
}
