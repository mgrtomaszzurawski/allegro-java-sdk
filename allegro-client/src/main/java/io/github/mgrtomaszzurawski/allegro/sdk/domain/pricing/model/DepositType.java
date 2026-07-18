/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DepositTypeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A deposit type the seller can attach to an offer (e.g. a returnable-packaging
 * or bottle deposit), as listed by {@code pricing().depositTypes()}.
 *
 * <p>Every field is spec-nullable, so any may be {@code null} on a sparse listing.
 *
 * @param id the deposit-type identifier
 * @param name the human-readable description
 * @param marketplaceId the marketplace the deposit type applies to
 * @param price the deposit amount, or {@code null} when the listing omits it
 *
 * @since 0.3.0
 */
public record DepositType(
        @Nullable UUID id,
        @Nullable String name,
        @Nullable String marketplaceId,
        @Nullable Money price) {

    /**
     * Map the generated response DTO to the public record.
     *
     * @param raw the generated deposit-type DTO
     * @return the mapped record
     */
    public static DepositType from(DepositTypeRaw raw) {
        return new DepositType(
                raw.getId(),
                raw.getName(),
                raw.getMarketplaceId(),
                raw.getPrice() == null
                        ? null
                        : Money.of(raw.getPrice().getAmount(), raw.getPrice().getCurrency()));
    }
}
