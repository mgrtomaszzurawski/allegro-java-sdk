/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceWithoutOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceChangeResult;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.UUID;

/**
 * The single-offer Buy Now price-change command, extracted from {@code OffersImpl} so the
 * command's {@code *Raw} request/response assembly does not add to that wrapper's coupling.
 * Allegro's price change is keyed by a client-generated command id; for one offer it resolves
 * synchronously, so a single PUT returns the terminal {@link PriceChangeResult}.
 */
final class ChangePriceCommand {

    private static final String OP_CHANGE_PRICE = "change offer Buy Now price";

    private ChangePriceCommand() {
    }

    /** Issue the price-change command and map its synchronous terminal result. */
    static PriceChangeResult apply(HttpSupport http, String offerId, Money buyNowPrice) {
        String commandId = UUID.randomUUID().toString();
        ChangePriceWithoutOutputRaw body = new ChangePriceWithoutOutputRaw()
                .id(commandId)
                .input(new ChangePriceInputRaw().buyNowPrice(
                        new PriceRaw().amount(buyNowPrice.amount()).currency(buyNowPrice.currency())));
        ChangePriceRaw response = http.putJsonAuthenticated(
                ApiPaths.changePriceCommand(offerId, commandId), body, ChangePriceRaw.class, OP_CHANGE_PRICE);
        return PriceChangeResult.from(response);
    }
}
