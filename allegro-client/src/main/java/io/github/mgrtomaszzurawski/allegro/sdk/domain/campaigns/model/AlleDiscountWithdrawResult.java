/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseInputRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The terminal result of an AlleDiscount {@code withdrawOffer} command. The SDK
 * polls the command to completion before returning, so {@link #status()} is
 * {@link AlleDiscountCommandStatus#SUCCESSFUL} or
 * {@link AlleDiscountCommandStatus#FAILED}. Alongside the outcome it echoes the
 * requested participation id and the command timestamps.
 *
 * @param commandId       the command id
 * @param status          terminal outcome
 * @param participationId the withdrawn participation id on success, else {@code null}
 * @param errors          coded failure reasons when {@code status} is FAILED; empty otherwise
 * @param requestedParticipationId the participation id the withdrawal was requested
 *                        for, or {@code null}
 * @param createdAt       when the command was created, or {@code null}
 * @param updatedAt       when the command was last updated, or {@code null}
 *
 * @since 0.2.0
 */
public record AlleDiscountWithdrawResult(
        String commandId,
        AlleDiscountCommandStatus status,
        @Nullable String participationId,
        List<ConditionViolation> errors,
        @Nullable String requestedParticipationId,
        @Nullable OffsetDateTime createdAt,
        @Nullable OffsetDateTime updatedAt) {

    public AlleDiscountWithdrawResult {
        errors = List.copyOf(errors);
    }

    /** Map the withdraw command's poll response to the public record. */
    public static AlleDiscountWithdrawResult from(AlleDiscountGetWithdrawCommandResponseRaw raw) {
        AlleDiscountGetWithdrawCommandResponseOutputRaw output = raw.getOutput();
        AlleDiscountGetWithdrawCommandResponseInputRaw input = raw.getInput();
        return new AlleDiscountWithdrawResult(
                raw.getId(),
                AlleDiscountCommandStatus.from(output.getStatus().name()),
                output.getWithdrawnOfferParticipation() == null
                        ? null
                        : output.getWithdrawnOfferParticipation().getParticipationId(),
                CampaignMappers.commandErrorViolations(output.getErrors()),
                input == null ? null : input.getParticipationId(),
                output.getCreatedAt(),
                output.getUpdatedAt());
    }
}
