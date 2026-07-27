/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueReasonRaw;
import org.jspecify.annotations.Nullable;

/**
 * The reason a buyer opened a post-purchase issue: the standardized {@code type}
 * the buyer picked plus their free-text elaboration.
 *
 * @param type standardized reason; never {@code null} (unmapped wire values map to
 *     {@link IssueReasonType#UNKNOWN})
 * @param description the buyer's free-text description, or {@code null}
 *
 * @since 0.2.0
 */
public record IssueReason(IssueReasonType type, @Nullable String description) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueReason from(PostPurchaseIssueReasonRaw raw) {
        return new IssueReason(IssueReasonType.from(raw.getType()), raw.getDescription());
    }
}
