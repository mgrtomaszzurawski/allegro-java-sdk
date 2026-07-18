/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompanyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MeResponseBaseMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MeResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Basic profile of the authenticated user, as returned by
 * {@code UserAccount.me()}.
 *
 * @param id numeric user identifier (as a string, per the Allegro contract)
 * @param login public login name
 * @param firstName first name, or {@code null} when the account has none set
 * @param lastName last name, or {@code null} when the account has none set
 * @param email account e-mail address
 * @param features account feature flags; never {@code null}, possibly empty
 * @param company business (VAT) registration data, or {@code null} for a
 *     personal account that carries none (added in 0.2.0)
 * @param baseMarketplaceId identifier of the marketplace the account is
 *     primarily registered on (e.g. {@code allegro-pl}), or {@code null} when
 *     the account declares none (added in 0.2.0)
 *
 * @since 0.1.0
 */
public record CurrentUser(
        String id,
        String login,
        @Nullable String firstName,
        @Nullable String lastName,
        String email,
        List<String> features,
        @Nullable Company company,
        @Nullable String baseMarketplaceId) {

    public CurrentUser {
        features = List.copyOf(features);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static CurrentUser from(MeResponseRaw raw) {
        List<String> rawFeatures = raw.getFeatures();
        return new CurrentUser(
                raw.getId(),
                raw.getLogin(),
                raw.getFirstName(),
                raw.getLastName(),
                raw.getEmail(),
                rawFeatures == null ? List.of() : rawFeatures,
                Company.from(raw.getCompany()),
                baseMarketplaceId(raw.getBaseMarketplace()));
    }

    private static @Nullable String baseMarketplaceId(@Nullable MeResponseBaseMarketplaceRaw raw) {
        return raw == null ? null : raw.getId();
    }

    /**
     * Business (VAT) registration data attached to a company account.
     *
     * @param name registered company name, or {@code null} when unset
     * @param taxId tax identification number (NIP), or {@code null} when unset
     *
     * @since 0.2.0
     */
    public record Company(@Nullable String name, @Nullable String taxId) {

        /** Map the generated Layer-1 DTO to the public record, or {@code null}. */
        public static @Nullable Company from(@Nullable CompanyRaw raw) {
            return raw == null ? null : new Company(raw.getName(), raw.getTaxId());
        }
    }
}
