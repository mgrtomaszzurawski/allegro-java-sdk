/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueRaw;
import org.jspecify.annotations.Nullable;

/**
 * Whether a post-purchase issue is a dispute or a claim.
 *
 * @since 0.2.0
 */
public enum IssueType {

    /** A dispute — a conversation about a problem with an order. */
    DISPUTE,
    /** A claim — a formal request with a resolvable status. */
    CLAIM,
    /** A type this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated issue type, tolerating unknown future values. */
    public static IssueType from(PostPurchaseIssueRaw.@Nullable TypeEnum raw) {
        if (raw == PostPurchaseIssueRaw.TypeEnum.DISPUTE) {
            return DISPUTE;
        }
        if (raw == PostPurchaseIssueRaw.TypeEnum.CLAIM) {
            return CLAIM;
        }
        return UNKNOWN;
    }
}
