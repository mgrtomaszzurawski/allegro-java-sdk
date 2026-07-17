/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeCampaignRaw;
import java.util.List;

/**
 * A badge campaign a seller can apply an offer to (for example the "Strefa
 * Okazji" bargain zone), as returned by {@code campaigns().badges().availableCampaigns()}.
 *
 * <p>Carries the seller's own eligibility for the campaign ({@link #eligible()}
 * and, when not eligible, the {@link #refusalReasons()}), so a caller can decide
 * whether applying is worthwhile before building a request.
 *
 * @param id                       campaign id (e.g. {@code "BARGAIN"})
 * @param name                     human-readable campaign name
 * @param marketplaceId            marketplace the campaign runs on (e.g. {@code "allegro-pl"})
 * @param type                     what participating offers gain
 * @param eligible                 whether the authenticated seller may join this campaign
 * @param refusalReasons           why the seller is not eligible; empty when {@link #eligible()} is {@code true}
 * @param application              when offers may be submitted to the campaign
 * @param visibility               when the campaign is visible in the seller's tools
 * @param publication              when the badge is displayed to buyers
 * @param regulationsLink          link to the campaign terms and conditions
 * @param stockReservationRequired whether the seller must declare campaign stock when applying
 *
 * @since 0.2.0
 */
public record BadgeCampaign(
        String id,
        String name,
        String marketplaceId,
        CampaignType type,
        boolean eligible,
        List<CampaignRefusalReason> refusalReasons,
        CampaignSchedule application,
        CampaignSchedule visibility,
        CampaignSchedule publication,
        String regulationsLink,
        boolean stockReservationRequired) {

    public BadgeCampaign {
        refusalReasons = List.copyOf(refusalReasons);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static BadgeCampaign from(BadgeCampaignRaw raw) {
        var eligibility = raw.getEligibility();
        List<CampaignRefusalReason> reasons = eligibility.getRefusalReasons().stream()
                .map(CampaignRefusalReason::from)
                .toList();
        Boolean stockReservation = raw.getStockReservationIsRequired();
        return new BadgeCampaign(
                raw.getId(),
                raw.getName(),
                raw.getMarketplace().getId(),
                CampaignType.from(raw.getType()),
                eligibility.getEligible(),
                reasons,
                schedule(raw.getApplication().getType().getValue(),
                        raw.getApplication().getFrom(), raw.getApplication().getTo()),
                schedule(raw.getVisibility().getType().getValue(),
                        raw.getVisibility().getFrom(), raw.getVisibility().getTo()),
                schedule(raw.getPublication().getType().getValue(),
                        raw.getPublication().getFrom(), raw.getPublication().getTo()),
                raw.getRegulationsLink(),
                stockReservation != null && stockReservation);
    }

    private static CampaignSchedule schedule(String policyType, String startIso, String endIso) {
        return CampaignSchedule.from(policyType, startIso, endIso);
    }
}
