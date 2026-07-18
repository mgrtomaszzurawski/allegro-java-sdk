/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageRaw;
import org.jspecify.annotations.Nullable;

/**
 * Delivery/processing status of a message.
 *
 * @since 0.2.0
 */
public enum MessageStatus {

    /** The message is being scanned before delivery. */
    VERIFYING,
    /** The message was blocked (e.g. flagged content). */
    BLOCKED,
    /** The message was delivered to the recipient. */
    DELIVERED,
    /** The message is part of an active interaction. */
    INTERACTING,
    /** The message was dismissed. */
    DISMISSED,
    /** A status this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated message status, tolerating unknown future values. */
    public static MessageStatus from(MessageRaw.@Nullable StatusEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case VERIFYING -> VERIFYING;
            case BLOCKED -> BLOCKED;
            case DELIVERED -> DELIVERED;
            case INTERACTING -> INTERACTING;
            case DISMISSED -> DISMISSED;
        };
    }
}
