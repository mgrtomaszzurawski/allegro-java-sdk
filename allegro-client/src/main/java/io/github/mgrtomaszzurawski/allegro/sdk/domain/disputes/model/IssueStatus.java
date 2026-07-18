/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueStatusRaw;
import org.jspecify.annotations.Nullable;

/**
 * Current status of a post-purchase issue. {@code DISPUTE_*} values apply to
 * disputes, {@code CLAIM_*} values to claims.
 *
 * @since 0.2.0
 */
public enum IssueStatus {

    /** The dispute was closed. */
    DISPUTE_CLOSED,
    /** The dispute is ongoing. */
    DISPUTE_ONGOING,
    /** The dispute ended without resolution. */
    DISPUTE_UNRESOLVED,
    /** The claim was submitted. */
    CLAIM_SUBMITTED,
    /** The claim was accepted. */
    CLAIM_ACCEPTED,
    /** The claim was rejected. */
    CLAIM_REJECTED,
    /** A status this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated issue status, tolerating unknown future values. */
    public static IssueStatus from(@Nullable PostPurchaseIssueStatusRaw raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case DISPUTE_CLOSED -> DISPUTE_CLOSED;
            case DISPUTE_ONGOING -> DISPUTE_ONGOING;
            case DISPUTE_UNRESOLVED -> DISPUTE_UNRESOLVED;
            case CLAIM_SUBMITTED -> CLAIM_SUBMITTED;
            case CLAIM_ACCEPTED -> CLAIM_ACCEPTED;
            case CLAIM_REJECTED -> CLAIM_REJECTED;
        };
    }
}
