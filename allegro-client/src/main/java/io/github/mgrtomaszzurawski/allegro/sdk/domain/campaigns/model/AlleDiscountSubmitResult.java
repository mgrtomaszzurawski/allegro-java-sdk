/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetSubmitCommandResponseOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetSubmitCommandResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The terminal result of an AlleDiscount {@code submitOffer} command. The SDK
 * polls the command to completion before returning, so {@link #status()} is
 * {@link AlleDiscountCommandStatus#SUCCESSFUL} or
 * {@link AlleDiscountCommandStatus#FAILED}.
 *
 * @param commandId       the command id
 * @param status          terminal outcome
 * @param participationId the created participation id on success, else {@code null}
 * @param errors          coded failure reasons when {@code status} is FAILED; empty otherwise
 *
 * @since 0.2.0
 */
public record AlleDiscountSubmitResult(
        String commandId,
        AlleDiscountCommandStatus status,
        @Nullable String participationId,
        List<ConditionViolation> errors) {

    public AlleDiscountSubmitResult {
        errors = List.copyOf(errors);
    }

    /** Map the submit command's poll response to the public record. */
    public static AlleDiscountSubmitResult from(AlleDiscountGetSubmitCommandResponseRaw raw) {
        AlleDiscountGetSubmitCommandResponseOutputRaw output = raw.getOutput();
        return new AlleDiscountSubmitResult(
                raw.getId(),
                AlleDiscountCommandStatus.from(output.getStatus().name()),
                output.getNewOfferParticipation() == null
                        ? null
                        : output.getNewOfferParticipation().getParticipationId(),
                CampaignMappers.commandErrorViolations(output.getErrors()));
    }
}
