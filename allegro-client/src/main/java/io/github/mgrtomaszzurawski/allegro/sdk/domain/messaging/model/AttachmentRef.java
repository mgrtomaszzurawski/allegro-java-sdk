/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageAttachmentIdRaw;

/**
 * A handle to a declared/uploaded message attachment. Obtain one from
 * {@code declareAttachment(...)}, upload the bytes with {@code uploadAttachment(...)},
 * then reference {@link #id()} when composing a message.
 *
 * @param id server-assigned attachment identifier
 *
 * @since 0.2.0
 */
public record AttachmentRef(String id) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static AttachmentRef from(MessageAttachmentIdRaw raw) {
        return new AttachmentRef(raw.getId());
    }
}
