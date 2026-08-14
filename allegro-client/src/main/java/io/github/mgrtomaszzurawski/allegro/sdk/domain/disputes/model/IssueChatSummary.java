/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueChatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueLastMessageRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A summary of an {@link Issue}'s chat carried on the issue itself: how many
 * messages it holds, the initial message, and when the last message arrived.
 *
 * @param messagesCount the number of messages in the chat, or {@code null} when absent
 * @param initialMessage the first message in the chat, or {@code null} when absent
 * @param lastMessageAt when the last message arrived, or {@code null} when absent
 *
 * @since 0.8.0
 */
public record IssueChatSummary(
        @Nullable Integer messagesCount,
        @Nullable IssueChatEntry initialMessage,
        @Nullable OffsetDateTime lastMessageAt) {

    /** Map the generated Layer-1 chat DTO, or {@code null} when absent. */
    public static @Nullable IssueChatSummary from(@Nullable PostPurchaseIssueChatRaw raw) {
        if (raw == null) {
            return null;
        }
        return new IssueChatSummary(
                raw.getMessagesCount(),
                raw.getInitialMessage() == null ? null : IssueChatEntry.from(raw.getInitialMessage()),
                lastMessageAt(raw.getLastMessage()));
    }

    private static @Nullable OffsetDateTime lastMessageAt(@Nullable PostPurchaseIssueLastMessageRaw raw) {
        return raw == null ? null : raw.getCreatedAt();
    }
}
