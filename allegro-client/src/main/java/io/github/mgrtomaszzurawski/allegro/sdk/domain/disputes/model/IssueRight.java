/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueRaw;
import org.jspecify.annotations.Nullable;

/**
 * The consumer right a post-purchase issue is raised under.
 *
 * @since 0.2.0
 */
public enum IssueRight {

    /** Raised under a warranty (gwarancja). */
    WARRANTY,
    /** Raised as a complaint (reklamacja / implied warranty). */
    COMPLAINT,
    /** A right this SDK release does not model yet. */
    UNKNOWN;

    /** Map the generated issue right, tolerating unknown future values. */
    public static IssueRight from(PostPurchaseIssueRaw.@Nullable RightEnum raw) {
        if (raw == PostPurchaseIssueRaw.RightEnum.WARRANTY) {
            return WARRANTY;
        }
        if (raw == PostPurchaseIssueRaw.RightEnum.COMPLAINT) {
            return COMPLAINT;
        }
        return UNKNOWN;
    }
}
