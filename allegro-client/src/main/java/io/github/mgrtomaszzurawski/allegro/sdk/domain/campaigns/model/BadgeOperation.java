/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationRaw;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * The outcome of a badge update, returned by {@code badges().update(...)}. The
 * SDK submits the change and polls the operation to a terminal state before
 * handing it back, so a returned {@code BadgeOperation} is always
 * {@link BadgeOperationStatus#PROCESSED} or {@link BadgeOperationStatus#DECLINED}.
 *
 * @param id               operation id
 * @param type             what the operation did to the badge
 * @param createdAt        when the operation was submitted
 * @param updatedAt        when the operation reached its terminal state
 * @param campaignId       the badge campaign
 * @param offerId          the offer whose badge was changed
 * @param status           terminal outcome
 * @param rejectionReasons why the operation was declined; empty unless
 *                         {@code status} is {@link BadgeOperationStatus#DECLINED}
 *
 * @since 0.2.0
 */
public record BadgeOperation(
        String id,
        BadgeOperationType type,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String campaignId,
        String offerId,
        BadgeOperationStatus status,
        List<CampaignRefusalReason> rejectionReasons) {

    public BadgeOperation {
        rejectionReasons = List.copyOf(rejectionReasons);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static BadgeOperation from(BadgeOperationRaw raw) {
        return new BadgeOperation(
                raw.getId(),
                BadgeOperationType.from(raw.getType()),
                OffsetDateTime.parse(raw.getCreatedAt()),
                OffsetDateTime.parse(raw.getUpdatedAt()),
                raw.getCampaign().getId(),
                raw.getOffer().getId(),
                BadgeOperationStatus.from(raw.getProcess().getStatus()),
                CampaignMappers.rejectionReasons(raw.getProcess().getRejectionReasons()));
    }
}
