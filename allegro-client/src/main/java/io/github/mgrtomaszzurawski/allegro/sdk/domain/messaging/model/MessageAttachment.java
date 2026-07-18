/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageAttachmentInfoRaw;
import org.jspecify.annotations.Nullable;

/**
 * An attachment carried by a message, as returned when reading a message.
 *
 * @param fileName the attachment's file name
 * @param mimeType the attachment's MIME type, or {@code null} when unknown
 * @param url a URL to fetch the attachment, or {@code null} when not exposed
 * @param status the malware-scan state; download is only safe when {@code SAFE}
 *
 * @since 0.2.0
 */
public record MessageAttachment(
        String fileName,
        @Nullable String mimeType,
        @Nullable String url,
        AttachmentStatus status) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static MessageAttachment from(MessageAttachmentInfoRaw raw) {
        return new MessageAttachment(
                raw.getFileName(),
                raw.getMimeType(),
                raw.getUrl(),
                AttachmentStatus.from(raw.getStatus()));
    }
}
