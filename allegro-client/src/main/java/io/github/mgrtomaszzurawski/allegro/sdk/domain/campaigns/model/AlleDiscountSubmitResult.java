/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetSubmitCommandResponseOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetSubmitCommandResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandResponseInputProposedPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountSubmitCommandResponseInputRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The terminal result of an AlleDiscount {@code submitOffer} command. The SDK
 * polls the command to completion before returning, so {@link #status()} is
 * {@link AlleDiscountCommandStatus#SUCCESSFUL} or
 * {@link AlleDiscountCommandStatus#FAILED}. Alongside the outcome it echoes the
 * command input (which offer was submitted to which campaign, at what proposed
 * price) and the command timestamps.
 *
 * @param commandId       the command id
 * @param status          terminal outcome
 * @param participationId the created participation id on success, else {@code null}
 * @param errors          coded failure reasons when {@code status} is FAILED; empty otherwise
 * @param submittedOfferId the id of the offer that was submitted, or {@code null}
 * @param campaignId      the id of the campaign it was submitted to, or {@code null}
 * @param proposedPrice   the proposed discount price, or {@code null}
 * @param createdAt       when the command was created, or {@code null}
 * @param updatedAt       when the command was last updated, or {@code null}
 *
 * @since 0.2.0
 */
public record AlleDiscountSubmitResult(
        String commandId,
        AlleDiscountCommandStatus status,
        @Nullable String participationId,
        List<ConditionViolation> errors,
        @Nullable String submittedOfferId,
        @Nullable String campaignId,
        @Nullable Money proposedPrice,
        @Nullable OffsetDateTime createdAt,
        @Nullable OffsetDateTime updatedAt) {

    public AlleDiscountSubmitResult {
        errors = List.copyOf(errors);
    }

    /** Map the submit command's poll response to the public record. */
    public static AlleDiscountSubmitResult from(AlleDiscountGetSubmitCommandResponseRaw raw) {
        AlleDiscountGetSubmitCommandResponseOutputRaw output = raw.getOutput();
        AlleDiscountSubmitCommandResponseInputRaw input = raw.getInput();
        return new AlleDiscountSubmitResult(
                raw.getId(),
                AlleDiscountCommandStatus.from(output.getStatus().name()),
                output.getNewOfferParticipation() == null
                        ? null
                        : output.getNewOfferParticipation().getParticipationId(),
                CampaignMappers.commandErrorViolations(output.getErrors()),
                input == null || input.getOffer() == null ? null : input.getOffer().getId(),
                input == null || input.getCampaign() == null ? null : input.getCampaign().getId(),
                input == null ? null : proposedPrice(input.getProposedPrice()),
                output.getCreatedAt(),
                output.getUpdatedAt());
    }

    private static @Nullable Money proposedPrice(
            @Nullable AlleDiscountSubmitCommandResponseInputProposedPriceRaw raw) {
        if (raw == null || raw.getAmount() == null || raw.getCurrency() == null) {
            return null;
        }
        return Money.of(raw.getAmount(), raw.getCurrency());
    }
}
