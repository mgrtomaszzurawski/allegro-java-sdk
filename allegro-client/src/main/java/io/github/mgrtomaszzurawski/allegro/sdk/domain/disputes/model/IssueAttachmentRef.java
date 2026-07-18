/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAttachmentIdRaw;

/**
 * A handle to an uploaded issue attachment. Obtain one from
 * {@code disputes.uploadAttachment(...)} and reference {@link #id()} in an
 * {@code IssueMessageRequest} to attach the file to a message.
 *
 * @param id the server-assigned attachment identifier
 *
 * @since 0.2.0
 */
public record IssueAttachmentRef(String id) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static IssueAttachmentRef from(PostPurchaseIssueAttachmentIdRaw raw) {
        return new IssueAttachmentRef(raw.getId());
    }
}
