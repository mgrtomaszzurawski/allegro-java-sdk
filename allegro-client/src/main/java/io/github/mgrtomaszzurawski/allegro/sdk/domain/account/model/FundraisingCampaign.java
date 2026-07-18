/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CharityOrganizationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FundraisingCampaignRaw;
import org.jspecify.annotations.Nullable;

/**
 * A charity fundraising campaign, as returned by {@code Charity.searchCampaigns(...)}.
 *
 * @param id campaign identifier
 * @param name campaign name
 * @param organizationName the fundraising organization's name, or {@code null}
 *
 * @since 0.2.0
 */
public record FundraisingCampaign(String id, @Nullable String name, @Nullable String organizationName) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static FundraisingCampaign from(FundraisingCampaignRaw raw) {
        CharityOrganizationRaw organization = raw.getOrganization();
        return new FundraisingCampaign(
                raw.getId(),
                raw.getName(),
                organization == null ? null : organization.getName());
    }
}
