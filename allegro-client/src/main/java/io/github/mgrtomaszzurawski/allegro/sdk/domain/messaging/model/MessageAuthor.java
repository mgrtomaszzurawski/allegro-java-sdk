/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageAuthorRaw;

/**
 * Author of a single message.
 *
 * @param login the author's public login
 * @param interlocutor {@code true} when the author is the conversation partner,
 *     {@code false} when it is the authenticated user
 *
 * @since 0.2.0
 */
public record MessageAuthor(String login, boolean interlocutor) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static MessageAuthor from(MessageAuthorRaw raw) {
        return new MessageAuthor(raw.getLogin(), Boolean.TRUE.equals(raw.getIsInterlocutor()));
    }
}
