/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CommandOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The synchronous result of a Buy Now price change
 * ({@link io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.Offers#changeBuyNowPrice}).
 * A single-offer price change resolves at once, so the consumer gets the command's
 * terminal outcome directly: the price it applied, the processing status, and any
 * per-field errors if it did not.
 *
 * @param id          the command identifier
 * @param buyNowPrice the Buy Now price the command applied (echoed by the server), or {@code null}
 * @param status      the terminal processing status token (e.g. {@code SUCCESSFUL},
 *                    {@code ERROR}), or {@code null}
 * @param errors      the structured errors for a failed change (possibly empty)
 * @since 0.7.0
 */
public record PriceChangeResult(
        @Nullable String id,
        @Nullable Money buyNowPrice,
        @Nullable String status,
        List<AllegroFieldError> errors) {

    public PriceChangeResult {
        errors = List.copyOf(errors);
    }

    /** Project the generated change-price command response onto the consumer record. */
    public static PriceChangeResult from(ChangePriceRaw raw) {
        CommandOutputRaw output = raw.getOutput();
        return new PriceChangeResult(
                raw.getId(),
                buyNowPriceOf(raw.getInput()),
                output == null || output.getStatus() == null ? null : output.getStatus().getValue(),
                errorsOf(output == null ? null : output.getErrors()));
    }

    private static @Nullable Money buyNowPriceOf(@Nullable ChangePriceInputRaw input) {
        PriceRaw price = input == null ? null : input.getBuyNowPrice();
        if (price == null || price.getAmount() == null || price.getCurrency() == null) {
            return null;
        }
        return Money.of(price.getAmount(), price.getCurrency());
    }

    private static List<AllegroFieldError> errorsOf(@Nullable List<ErrorRaw> raws) {
        return raws == null
                ? List.of()
                : raws.stream()
                        .map(raw -> new AllegroFieldError(raw.getCode(), raw.getMessage(),
                                raw.getUserMessage(), raw.getPath(), raw.getDetails()))
                        .toList();
    }
}
