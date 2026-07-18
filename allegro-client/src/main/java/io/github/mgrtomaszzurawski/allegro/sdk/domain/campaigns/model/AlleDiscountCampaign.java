/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerApplicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerPublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerVisibilityRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * An AlleDiscount campaign the seller can submit offers to, returned by
 * {@code alleDiscount().campaigns()}. Exposes the campaign's type and its three
 * time windows (when offers may be submitted, when the campaign is visible, and
 * when the discount shows to buyers).
 *
 * @param id           campaign id
 * @param name         campaign display name
 * @param marketplaceId the marketplace the campaign runs on
 * @param type         the campaign kind
 * @param application  when offers may be submitted, or {@code null}
 * @param visibility   when the campaign is visible in the seller's tools, or {@code null}
 * @param publication  when the discount is shown to buyers, or {@code null}
 *
 * @since 0.2.0
 */
public record AlleDiscountCampaign(
        String id,
        String name,
        String marketplaceId,
        AlleDiscountCampaignType type,
        @Nullable CampaignSchedule application,
        @Nullable CampaignSchedule visibility,
        @Nullable CampaignSchedule publication) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static AlleDiscountCampaign from(
            AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw raw) {
        return new AlleDiscountCampaign(
                raw.getId(),
                raw.getName(),
                raw.getMarketplace().getId(),
                AlleDiscountCampaignType.from(raw.getType()),
                application(raw.getApplication()),
                visibility(raw.getVisibility()),
                publication(raw.getPublication()));
    }

    private static CampaignSchedule schedule(String policyType,
            @Nullable OffsetDateTime startsAt, @Nullable OffsetDateTime endsAt) {
        return new CampaignSchedule(SchedulePolicyType.from(policyType), startsAt, endsAt);
    }

    private static @Nullable CampaignSchedule application(
            @Nullable AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerApplicationRaw policy) {
        return policy == null ? null : schedule(policy.getType().getValue(), policy.getFrom(), policy.getTo());
    }

    private static @Nullable CampaignSchedule visibility(
            @Nullable AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerVisibilityRaw policy) {
        return policy == null ? null : schedule(policy.getType().getValue(), policy.getFrom(), policy.getTo());
    }

    private static @Nullable CampaignSchedule publication(
            @Nullable AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerPublicationRaw policy) {
        return policy == null ? null : schedule(policy.getType().getValue(), policy.getFrom(), policy.getTo());
    }
}
