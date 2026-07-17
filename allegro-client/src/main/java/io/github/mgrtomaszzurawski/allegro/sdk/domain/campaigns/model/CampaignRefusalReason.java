/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CampaignRefusalReasonRaw;
import java.util.List;

/**
 * Why the authenticated seller is not eligible for a campaign: a stable
 * {@code code} plus the messages that explain it. Present on a
 * {@link BadgeCampaign} only when {@link BadgeCampaign#eligible()} is {@code false}.
 *
 * @param code     stable refusal code (see the Allegro badge documentation)
 * @param messages explanatory messages; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record CampaignRefusalReason(String code, List<RefusalMessage> messages) {

    public CampaignRefusalReason {
        messages = List.copyOf(messages);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    static CampaignRefusalReason from(CampaignRefusalReasonRaw raw) {
        return new CampaignRefusalReason(
                raw.getCode(),
                raw.getMessages().stream().map(RefusalMessage::from).toList());
    }
}
