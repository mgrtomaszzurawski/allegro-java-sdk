/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageRaw;
import org.jspecify.annotations.Nullable;

/**
 * Origin/kind of a message.
 *
 * @since 0.2.0
 */
public enum MessageType {

    /** A pre-purchase question about an offer. */
    ASK_QUESTION,
    /** A message that originated as e-mail. */
    MAIL,
    /** A message exchanged through the Allegro message center. */
    MESSAGE_CENTER,
    /** A type this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated message type, tolerating unknown future values. */
    public static MessageType from(MessageRaw.@Nullable TypeEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case ASK_QUESTION -> ASK_QUESTION;
            case MAIL -> MAIL;
            case MESSAGE_CENTER -> MESSAGE_CENTER;
            default -> UNKNOWN;
        };
    }
}
