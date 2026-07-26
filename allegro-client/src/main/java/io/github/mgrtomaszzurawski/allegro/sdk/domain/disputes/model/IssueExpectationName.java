/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueExpectationRaw;
import org.jspecify.annotations.Nullable;

/**
 * What the buyer expects the seller to do to resolve a post-purchase issue.
 *
 * @since 0.2.0
 */
public enum IssueExpectationName {

    /** Repair the product. */
    REPAIR,
    /** Exchange the product. */
    EXCHANGE,
    /** Refund the full amount. */
    REFUND,
    /** Refund part of the amount. */
    PARTIAL_REFUND,
    /** An expectation this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated expectation name, tolerating unknown future values. */
    public static IssueExpectationName from(PostPurchaseIssueExpectationRaw.@Nullable NameEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case REPAIR -> REPAIR;
            case EXCHANGE -> EXCHANGE;
            case REFUND -> REFUND;
            case PARTIAL_REFUND -> PARTIAL_REFUND;
            default -> UNKNOWN;
        };
    }
}
