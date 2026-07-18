/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationRejectionReasonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CampaignRefusalReasonRaw;
import java.util.List;

/**
 * A coded reason with human-readable messages. Used both when the authenticated
 * seller is not eligible for a campaign (present on a {@link BadgeCampaign} only
 * when {@link BadgeCampaign#eligible()} is {@code false}) and when a badge
 * application or update operation is {@code DECLINED}.
 *
 * @param code     stable reason code (see the Allegro badge documentation)
 * @param messages explanatory messages; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record CampaignRefusalReason(String code, List<RefusalMessage> messages) {

    public CampaignRefusalReason {
        messages = List.copyOf(messages);
    }

    /** Map the campaign-ineligibility Layer-1 DTO to the public immutable record. */
    static CampaignRefusalReason from(CampaignRefusalReasonRaw raw) {
        return new CampaignRefusalReason(
                raw.getCode(),
                raw.getMessages().stream().map(RefusalMessage::from).toList());
    }

    /** Map the badge application/operation rejection Layer-1 DTO to the public record. */
    static CampaignRefusalReason from(BadgeApplicationRejectionReasonRaw raw) {
        return new CampaignRefusalReason(
                raw.getCode(),
                raw.getMessages().stream().map(RefusalMessage::from).toList());
    }
}
