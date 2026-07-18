/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AlleDiscountGetWithdrawCommandResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The terminal result of an AlleDiscount {@code withdrawOffer} command. The SDK
 * polls the command to completion before returning, so {@link #status()} is
 * {@link AlleDiscountCommandStatus#SUCCESSFUL} or
 * {@link AlleDiscountCommandStatus#FAILED}.
 *
 * @param commandId       the command id
 * @param status          terminal outcome
 * @param participationId the withdrawn participation id on success, else {@code null}
 * @param errors          failure messages when {@code status} is FAILED; empty otherwise
 *
 * @since 0.2.0
 */
public record AlleDiscountWithdrawResult(
        String commandId,
        AlleDiscountCommandStatus status,
        @Nullable String participationId,
        List<String> errors) {

    public AlleDiscountWithdrawResult {
        errors = List.copyOf(errors);
    }

    /** Map the withdraw command's poll response to the public record. */
    public static AlleDiscountWithdrawResult from(AlleDiscountGetWithdrawCommandResponseRaw raw) {
        AlleDiscountGetWithdrawCommandResponseOutputRaw output = raw.getOutput();
        return new AlleDiscountWithdrawResult(
                raw.getId(),
                AlleDiscountCommandStatus.from(output.getStatus().name()),
                output.getWithdrawnOfferParticipation() == null
                        ? null
                        : output.getWithdrawnOfferParticipation().getParticipationId(),
                CampaignMappers.commandErrorMessages(output.getErrors()));
    }
}
