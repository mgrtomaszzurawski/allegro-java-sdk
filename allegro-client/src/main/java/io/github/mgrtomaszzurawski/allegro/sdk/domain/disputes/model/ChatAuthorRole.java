/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAuthorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueMessageAuthorRaw;
import org.jspecify.annotations.Nullable;

/**
 * The role of the author of a post-purchase issue chat entry.
 *
 * @since 0.2.0
 */
public enum ChatAuthorRole {

    /** The buyer who opened the issue. */
    BUYER,
    /** The seller. */
    SELLER,
    /** An Allegro administrator mediating the issue. */
    ADMIN,
    /** An automated system entry (e.g. a status change). */
    SYSTEM,
    /** The One Fulfillment operation. */
    FULFILLMENT,
    /** A role this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated author role, tolerating unknown future values. */
    public static ChatAuthorRole from(PostPurchaseIssueMessageAuthorRaw.@Nullable RoleEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case BUYER -> BUYER;
            case SELLER -> SELLER;
            case ADMIN -> ADMIN;
            case SYSTEM -> SYSTEM;
            case FULFILLMENT -> FULFILLMENT;
            default -> UNKNOWN;
        };
    }

    /**
     * Map the generated issue-author role (the variant carried by the issue chat's
     * initial message), tolerating unknown future values.
     *
     * @param raw the generated role, or {@code null}
     * @return the matching role, or {@link #UNKNOWN}
     */
    public static ChatAuthorRole from(PostPurchaseIssueAuthorRaw.@Nullable RoleEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case BUYER -> BUYER;
            case ADMIN -> ADMIN;
            default -> UNKNOWN;
        };
    }
}
