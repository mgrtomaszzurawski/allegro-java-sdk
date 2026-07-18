/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRejectionReasonMessageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RefusalMessageRaw;
import org.jspecify.annotations.Nullable;

/**
 * One human-readable reason, with an optional help link. Grouped under a
 * {@link CampaignRefusalReason} code — used both for campaign ineligibility and
 * for badge application/operation rejections.
 *
 * @param text human-readable explanation
 * @param link optional page with more detail, or {@code null}
 *
 * @since 0.2.0
 */
public record RefusalMessage(String text, @Nullable String link) {

    /** Map the campaign-ineligibility Layer-1 DTO to the public immutable record. */
    static RefusalMessage from(RefusalMessageRaw raw) {
        return new RefusalMessage(raw.getText(), raw.getLink());
    }

    /** Map the badge application/operation rejection Layer-1 DTO to the public record. */
    static RefusalMessage from(BadgeApplicationRejectionReasonMessageRaw raw) {
        return new RefusalMessage(raw.getText(), raw.getLink());
    }
}
