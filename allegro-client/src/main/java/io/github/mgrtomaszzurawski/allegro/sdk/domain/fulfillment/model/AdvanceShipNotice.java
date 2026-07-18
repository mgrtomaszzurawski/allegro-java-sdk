/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdvanceShipNoticeListItemResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdvanceShipNoticeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CreateAdvanceShipNoticeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.HandlingUnitRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.LabelsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductItemRaw;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An Advance Ship Notice (ASN) — the seller's declaration of goods being sent
 * into a One Fulfillment warehouse. Read via {@code advanceShipNotices().get(id)}
 * or streamed from {@code advanceShipNotices().streamNotices(...)}, and returned
 * by the create / update writes.
 *
 * <p>{@link #version()} carries the server's current {@code ETag} for this notice
 * when the record came from a single-notice read or write; pass it to
 * {@code update(...)} / {@code updateSubmitted(...)} as the optimistic-concurrency
 * token. It is {@code null} on rows streamed from the list, which do not carry a
 * per-row version. {@link #volumeInCc()} is likewise present only on single-notice
 * reads/writes.
 *
 * <p>The notice's {@code shipping} declaration (the polymorphic
 * {@code COURIER_BY_SELLER} / {@code OWN_TRANSPORT} / {@code THIRD_PARTY_DELIVERY}
 * variants, each with its own courier / carrier / arrival details) is a deferred
 * follow-up and is not yet modelled here.
 *
 * @param id            the notice identifier (a UUID)
 * @param displayNumber the human-readable notice number
 * @param status        lifecycle status
 * @param createdAt     when the notice was created
 * @param updatedAt     when the notice was last changed
 * @param items         the product lines (never {@code null}; may be empty)
 * @param handlingUnit  how the goods are packed, when declared
 * @param labelsFileUrl a server-provided URL to the notice's printed labels, when
 *                      available; to download the labels use
 *                      {@code advanceShipNotices().labels(id)} (authenticated) rather
 *                      than fetching this URL with your own credentials
 * @param submittedAt   when the notice was submitted, once it has been
 * @param volumeInCc    the notice's volume in cubic centimetres, when known
 * @param version       the optimistic-concurrency token ({@code ETag}), when known
 *
 * @since 0.4.0
 */
public record AdvanceShipNotice(
        String id,
        String displayNumber,
        AsnStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AsnItem> items,
        @Nullable HandlingUnit handlingUnit,
        @Nullable String labelsFileUrl,
        @Nullable OffsetDateTime submittedAt,
        @Nullable BigDecimal volumeInCc,
        @Nullable String version) {

    /** Map a full single-notice response, carrying the response {@code ETag} as the version. */
    public static AdvanceShipNotice from(AdvanceShipNoticeResponseRaw raw, @Nullable String version) {
        return new AdvanceShipNotice(
                raw.getId().toString(),
                raw.getDisplayNumber(),
                AsnStatus.fromWire(raw.getStatus().getValue()),
                raw.getCreatedAt(),
                raw.getUpdatedAt(),
                mapItems(raw.getItems()),
                mapHandlingUnit(raw.getHandlingUnit()),
                labelsUrl(raw.getLabels()),
                raw.getSubmittedAt(),
                raw.getVolumeInCc(),
                version);
    }

    /** Map a create response, carrying the response {@code ETag} as the version. */
    public static AdvanceShipNotice from(CreateAdvanceShipNoticeResponseRaw raw, @Nullable String version) {
        return new AdvanceShipNotice(
                raw.getId().toString(),
                raw.getDisplayNumber(),
                AsnStatus.fromWire(raw.getStatus().getValue()),
                raw.getCreatedAt(),
                raw.getUpdatedAt(),
                mapItems(raw.getItems()),
                mapHandlingUnit(raw.getHandlingUnit()),
                labelsUrl(raw.getLabels()),
                null,
                raw.getVolumeInCc(),
                version);
    }

    /** Map a list row — no per-row version or volume is sent. */
    public static AdvanceShipNotice from(AdvanceShipNoticeListItemResponseRaw raw) {
        return new AdvanceShipNotice(
                raw.getId().toString(),
                raw.getDisplayNumber(),
                AsnStatus.fromWire(raw.getStatus().getValue()),
                raw.getCreatedAt(),
                raw.getUpdatedAt(),
                mapItems(raw.getItems()),
                mapHandlingUnit(raw.getHandlingUnit()),
                labelsUrl(raw.getLabels()),
                raw.getSubmittedAt(),
                null,
                null);
    }

    private static List<AsnItem> mapItems(@Nullable List<ProductItemRaw> items) {
        return items == null ? List.of() : items.stream().map(AsnItem::from).toList();
    }

    private static @Nullable HandlingUnit mapHandlingUnit(@Nullable HandlingUnitRaw raw) {
        return raw == null ? null : HandlingUnit.from(raw);
    }

    private static @Nullable String labelsUrl(@Nullable LabelsRaw raw) {
        return raw == null ? null : raw.getFileUrl();
    }
}
