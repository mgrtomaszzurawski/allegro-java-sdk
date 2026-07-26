/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAttachmentRaw;
import org.jspecify.annotations.Nullable;

/**
 * A file attached to a post-purchase issue — either to the issue itself (the
 * evidence the buyer supplied when opening it) or to a chat entry.
 *
 * @param fileName the attachment's file name, or {@code null}
 * @param url a URL to fetch the attachment, or {@code null}
 *
 * @since 0.2.0
 */
public record IssueAttachment(@Nullable String fileName, @Nullable String url) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueAttachment from(PostPurchaseIssueAttachmentRaw raw) {
        return new IssueAttachment(raw.getFileName(), raw.getUrl());
    }
}
