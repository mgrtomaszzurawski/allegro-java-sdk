/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.InterlocutorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThreadRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A message-center conversation thread between the authenticated user and one
 * interlocutor.
 *
 * @param id server-assigned thread identifier
 * @param read whether the thread has no unread messages for the authenticated user
 * @param lastMessageDateTime timestamp of the most recent message, or {@code null}
 *     when the thread carries none
 * @param interlocutor the conversation partner, or {@code null} when the server
 *     does not disclose it
 *
 * @since 0.2.0
 */
public record MessageThread(
        String id,
        boolean read,
        @Nullable OffsetDateTime lastMessageDateTime,
        @Nullable Interlocutor interlocutor) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static MessageThread from(ThreadRaw raw) {
        InterlocutorRaw interlocutor = raw.getInterlocutor();
        return new MessageThread(
                raw.getId(),
                Boolean.TRUE.equals(raw.getRead()),
                raw.getLastMessageDateTime(),
                interlocutor == null ? null : Interlocutor.from(interlocutor));
    }
}
