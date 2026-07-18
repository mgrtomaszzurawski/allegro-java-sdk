/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.InterlocutorRaw;

/**
 * The other party of a message thread (the conversation partner of the
 * authenticated user).
 *
 * @param login the interlocutor's public login
 * @param avatarUrl URL of the interlocutor's avatar
 *
 * @since 0.2.0
 */
public record Interlocutor(String login, String avatarUrl) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Interlocutor from(InterlocutorRaw raw) {
        return new Interlocutor(raw.getLogin(), raw.getAvatarUrl());
    }
}
