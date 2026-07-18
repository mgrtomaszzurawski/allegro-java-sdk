/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageAttachmentInfoRaw;
import org.jspecify.annotations.Nullable;

/**
 * Malware-scan state of a message attachment.
 *
 * @since 0.2.0
 */
public enum AttachmentStatus {

    /** Uploaded, scan not yet completed (server value {@code NEW}). */
    PENDING,
    /** Scanned and considered safe to download. */
    SAFE,
    /** Scanned and flagged as unsafe. */
    UNSAFE,
    /** The attachment is no longer available. */
    EXPIRED,
    /** A status this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated attachment status, tolerating unknown future values. */
    public static AttachmentStatus from(MessageAttachmentInfoRaw.@Nullable StatusEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case NEW -> PENDING;
            case SAFE -> SAFE;
            case UNSAFE -> UNSAFE;
            case EXPIRED -> EXPIRED;
            default -> UNKNOWN;
        };
    }
}
