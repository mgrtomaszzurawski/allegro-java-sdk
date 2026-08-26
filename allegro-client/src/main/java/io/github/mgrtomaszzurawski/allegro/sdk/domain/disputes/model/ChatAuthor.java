/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAuthorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueMessageAuthorRaw;
import org.jspecify.annotations.Nullable;

/**
 * The author of a post-purchase issue chat entry.
 *
 * @param login the author's public login, or {@code null}
 * @param role the author's role in the issue
 *
 * @since 0.2.0
 */
public record ChatAuthor(@Nullable String login, ChatAuthorRole role) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static ChatAuthor from(PostPurchaseIssueMessageAuthorRaw raw) {
        return new ChatAuthor(raw.getLogin(), ChatAuthorRole.from(raw.getRole()));
    }

    /**
     * Map the issue-author DTO (the variant carried by the chat's initial message)
     * to the public immutable record.
     *
     * @param raw the generated author DTO
     * @return the mapped author
     */
    public static ChatAuthor from(PostPurchaseIssueAuthorRaw raw) {
        return new ChatAuthor(raw.getLogin(), ChatAuthorRole.from(raw.getRole()));
    }
}
