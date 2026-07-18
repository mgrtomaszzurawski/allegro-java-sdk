/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueStateRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * The current state of a post-purchase issue.
 *
 * @param status the current status
 * @param statusDueDate when the current status must change by, or {@code null}
 * @param returnRequired whether the buyer must return the goods
 * @param chatActive whether the chat is still open for new messages
 *
 * @since 0.2.0
 */
public record IssueState(
        IssueStatus status,
        @Nullable OffsetDateTime statusDueDate,
        boolean returnRequired,
        boolean chatActive) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueState from(PostPurchaseIssueStateRaw raw) {
        return new IssueState(
                IssueStatus.from(raw.getStatus()),
                raw.getStatusDueDate(),
                Boolean.TRUE.equals(raw.getReturnRequired()),
                Boolean.TRUE.equals(raw.getChatActive()));
    }
}
