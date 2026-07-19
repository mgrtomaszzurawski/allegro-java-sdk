/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmittedOfferDtoProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeCampaignRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationProcessRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgeProcessRaw;
import org.junit.jupiter.api.Test;

/**
 * Forward-compatibility proof for the campaigns domain enums (C3). Every enum maps
 * a known wire value to its constant and degrades an unrecognized wire value to
 * {@code UNKNOWN} instead of throwing — the behaviour the generated Layer-1 enums,
 * which surface an unknown value as the {@code UNKNOWN_DEFAULT_OPEN_API} sentinel,
 * cannot provide once the value is re-mapped by name. Enums fed from a generated
 * enum are exercised with that sentinel; enums fed from a raw string are exercised
 * with an unseen token.
 */
class CampaignsEnumsTest {

    private static final String UNSEEN_TOKEN = "SOMETHING_ALLEGRO_ADDED_LATER";

    @Test
    void campaignType_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known badge campaign type and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(CampaignType.DISCOUNT, CampaignType.from(BadgeCampaignRaw.TypeEnum.DISCOUNT));
        assertEquals(CampaignType.UNKNOWN,
                CampaignType.from(BadgeCampaignRaw.TypeEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void alleDiscountCampaignType_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known AlleDiscount campaign type and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(AlleDiscountCampaignType.SOURCING, AlleDiscountCampaignType.from(
                AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw.TypeEnum.SOURCING));
        assertEquals(AlleDiscountCampaignType.UNKNOWN, AlleDiscountCampaignType.from(
                AlleDiscountListCampaignsResponseAlleDiscountCampaignsInnerRaw.TypeEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void alleDiscountOfferStatus_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known submitted-offer status and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(AlleDiscountOfferStatus.ACTIVE, AlleDiscountOfferStatus.from(
                AlleDiscountSubmittedOfferDtoProcessRaw.StatusEnum.ACTIVE));
        assertEquals(AlleDiscountOfferStatus.UNKNOWN, AlleDiscountOfferStatus.from(
                AlleDiscountSubmittedOfferDtoProcessRaw.StatusEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void badgeApplicationStatus_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known badge-application status and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(BadgeApplicationStatus.REQUESTED, BadgeApplicationStatus.from(
                BadgeApplicationProcessRaw.StatusEnum.REQUESTED));
        assertEquals(BadgeApplicationStatus.UNKNOWN, BadgeApplicationStatus.from(
                BadgeApplicationProcessRaw.StatusEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void badgeOperationStatus_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known badge-operation status and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(BadgeOperationStatus.PROCESSED, BadgeOperationStatus.from(
                BadgeOperationProcessRaw.StatusEnum.PROCESSED));
        assertEquals(BadgeOperationStatus.UNKNOWN, BadgeOperationStatus.from(
                BadgeOperationProcessRaw.StatusEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void badgeOperationType_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known badge-operation type and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(BadgeOperationType.UPDATE, BadgeOperationType.from(BadgeOperationRaw.TypeEnum.UPDATE));
        assertEquals(BadgeOperationType.UNKNOWN,
                BadgeOperationType.from(BadgeOperationRaw.TypeEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void badgeStatus_mapsKnownAndDegradesSentinelToUnknown() {
        // given a known badge status and the Layer-1 unknown sentinel
        // when each is mapped to the domain enum
        // then the known one maps and the sentinel degrades to UNKNOWN, never throwing
        assertEquals(BadgeStatus.ACTIVE, BadgeStatus.from(BadgeProcessRaw.StatusEnum.ACTIVE));
        assertEquals(BadgeStatus.UNKNOWN, BadgeStatus.from(BadgeProcessRaw.StatusEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void alleDiscountCommandStatus_mapsKnownAndDegradesUnseenTokenToUnknown() {
        // given a known command status token and one this build has never seen
        // when each is mapped to the domain enum
        // then the known one maps and the unseen one degrades to UNKNOWN, never throwing
        assertEquals(AlleDiscountCommandStatus.SUCCESSFUL, AlleDiscountCommandStatus.from("SUCCESSFUL"));
        assertEquals(AlleDiscountCommandStatus.UNKNOWN, AlleDiscountCommandStatus.from(UNSEEN_TOKEN));
    }

    @Test
    void participationStatus_mapsKnownAndDegradesUnseenTokenToUnknown() {
        // given a known participation token and one this build has never seen
        // when each is mapped to the domain enum
        // then the known one maps and the unseen one degrades to UNKNOWN, never throwing
        assertEquals(ParticipationStatus.ALLOWED, ParticipationStatus.from("ALLOWED"));
        assertEquals(ParticipationStatus.UNKNOWN, ParticipationStatus.from(UNSEEN_TOKEN));
    }

    @Test
    void schedulePolicyType_mapsKnownAndDegradesUnseenTokenToUnknown() {
        // given a known schedule-policy token and one this build has never seen
        // when each is mapped to the domain enum
        // then the known one maps and the unseen one degrades to UNKNOWN, never throwing
        assertEquals(SchedulePolicyType.WITHIN, SchedulePolicyType.from("WITHIN"));
        assertEquals(SchedulePolicyType.UNKNOWN, SchedulePolicyType.from(UNSEEN_TOKEN));
    }

    @Test
    void subsidyOfferStatus_mapsKnownAndDegradesUnseenTokenToUnknown() {
        // given a known subsidy-offer token and one this build has never seen
        // when each is mapped to the domain enum
        // then the known one maps and the unseen one degrades to UNKNOWN, never throwing
        assertEquals(SubsidyOfferStatus.SUCCESS, SubsidyOfferStatus.from("SUCCESS"));
        assertEquals(SubsidyOfferStatus.UNKNOWN, SubsidyOfferStatus.from(UNSEEN_TOKEN));
    }
}
