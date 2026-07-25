/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentFileRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAttachmentRaw;
import org.jspecify.annotations.Nullable;

/**
 * An offer attachment — a document (manual, energy label, …) referenced by an offer.
 * Created by declaring it ({@code createAttachment}) and then uploading the file bytes
 * ({@code uploadAttachment}); the {@link #fileUrl() hosted URL} is populated once the
 * upload completes.
 *
 * <p>The {@link #uploadUrl() upload URL} is the one-time address returned in the
 * declaration's {@code Location} header — Allegro's docs say to PUT the file bytes to
 * exactly that URL rather than composing it, so {@code uploadAttachment} takes the whole
 * declared attachment (which carries it). It is {@code null} on a read.
 *
 * @param id        the attachment id
 * @param type      the document kind
 * @param fileName  the uploaded file's name, or {@code null} before upload
 * @param fileUrl   the hosted file URL, or {@code null} before the upload completes
 * @param uploadUrl the one-time URL to PUT the file to (from the declaration), or {@code null}
 * @since 0.4.0
 */
public record OfferAttachment(
        @Nullable String id,
        AttachmentType type,
        @Nullable String fileName,
        @Nullable String fileUrl,
        @Nullable String uploadUrl) {

    /** Project a generated offer attachment onto the consumer record (no upload URL). */
    public static OfferAttachment from(OfferAttachmentRaw raw) {
        return from(raw, null);
    }

    /** Project a generated offer attachment, carrying the declaration's one-time upload URL. */
    public static OfferAttachment from(OfferAttachmentRaw raw, @Nullable String uploadUrl) {
        AttachmentFileRaw file = raw.getFile();
        return new OfferAttachment(
                raw.getId(),
                AttachmentType.from(raw.getType()),
                file == null ? null : file.getName(),
                file == null ? null : file.getUrl(),
                uploadUrl);
    }
}
