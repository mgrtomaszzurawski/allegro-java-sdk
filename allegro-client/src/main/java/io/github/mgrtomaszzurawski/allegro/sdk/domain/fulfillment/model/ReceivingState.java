/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReceivingEntryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReceivingStateRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The warehouse's receiving progress for an Advance Ship Notice: how far
 * unpacking has got and, per product, what was expected versus received. Read
 * via {@code advanceShipNotices().receivingState(id)}.
 *
 * @param updatedAt     when the receiving state was last updated, when reported
 * @param stage         how far receiving has progressed, when reported
 * @param displayNumber the human-readable notice number, when reported
 * @param content       the per-product receiving lines (never {@code null}; may be empty)
 *
 * @since 0.4.0
 */
public record ReceivingState(
        @Nullable OffsetDateTime updatedAt,
        @Nullable ReceivingStage stage,
        @Nullable String displayNumber,
        List<ReceivingEntry> content) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReceivingState from(ReceivingStateRaw raw) {
        List<ReceivingEntryRaw> content = raw.getContent();
        return new ReceivingState(
                raw.getUpdatedAt(),
                raw.getStage() == null ? null : ReceivingStage.fromWire(raw.getStage().getValue()),
                raw.getDisplayNumber(),
                content == null ? List.of() : content.stream().map(ReceivingEntry::from).toList());
    }
}
