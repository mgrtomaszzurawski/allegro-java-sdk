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
 * @param id       the attachment id (to reference it and to upload the file to)
 * @param type     the document kind
 * @param fileName the uploaded file's name, or {@code null} before upload
 * @param fileUrl  the hosted file URL, or {@code null} before the upload completes
 * @since 0.4.0
 */
public record OfferAttachment(
        @Nullable String id,
        AttachmentType type,
        @Nullable String fileName,
        @Nullable String fileUrl) {

    /** Project a generated offer attachment onto the consumer record. */
    public static OfferAttachment from(OfferAttachmentRaw raw) {
        AttachmentFileRaw file = raw.getFile();
        return new OfferAttachment(
                raw.getId(),
                AttachmentType.from(raw.getType()),
                file == null ? null : file.getName(),
                file == null ? null : file.getUrl());
    }
}
