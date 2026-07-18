/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueUserRaw;
import org.jspecify.annotations.Nullable;

/**
 * A party to a post-purchase issue (the buyer who opened it).
 *
 * @param id the party's user id, or {@code null}
 * @param login the party's public login, or {@code null}
 *
 * @since 0.2.0
 */
public record IssueParticipant(@Nullable String id, @Nullable String login) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueParticipant from(PostPurchaseIssueUserRaw raw) {
        return new IssueParticipant(raw.getId(), raw.getLogin());
    }
}
