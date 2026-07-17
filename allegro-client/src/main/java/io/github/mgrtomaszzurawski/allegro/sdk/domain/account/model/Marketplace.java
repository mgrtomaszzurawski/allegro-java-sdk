/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemCurrenciesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemCurrencyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemLanguageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemLanguagesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MarketplaceItemShippingCountryRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One Allegro marketplace (e.g. {@code allegro-pl}, {@code allegro-cz}), as
 * returned by {@code Marketplaces.list()}. Nested language/currency/country
 * objects are flattened to their code strings — the codes are what a consumer
 * uses when creating offers on, or shipping to, a marketplace.
 *
 * @param id marketplace identifier (e.g. {@code allegro-pl})
 * @param offerCreationLanguages BCP-47 codes an offer may be written in; never
 *     {@code null}, possibly empty
 * @param offerDisplayLanguages BCP-47 codes a buyer may see the offer in; never
 *     {@code null}, possibly empty
 * @param baseCurrency ISO-4217 code of the marketplace's base currency, or
 *     {@code null} when the marketplace declares none
 * @param additionalCurrencies other ISO-4217 codes accepted on the marketplace;
 *     never {@code null}, possibly empty
 * @param shippingCountries ISO country codes the marketplace ships to; never
 *     {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record Marketplace(
        String id,
        List<String> offerCreationLanguages,
        List<String> offerDisplayLanguages,
        @Nullable String baseCurrency,
        List<String> additionalCurrencies,
        List<String> shippingCountries) {

    public Marketplace {
        offerCreationLanguages = List.copyOf(offerCreationLanguages);
        offerDisplayLanguages = List.copyOf(offerDisplayLanguages);
        additionalCurrencies = List.copyOf(additionalCurrencies);
        shippingCountries = List.copyOf(shippingCountries);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Marketplace from(MarketplaceItemRaw raw) {
        MarketplaceItemLanguagesRaw languages = raw.getLanguages();
        MarketplaceItemCurrenciesRaw currencies = raw.getCurrencies();
        return new Marketplace(
                raw.getId(),
                languageCodes(languages == null ? null : languages.getOfferCreation()),
                languageCodes(languages == null ? null : languages.getOfferDisplay()),
                baseCurrencyCode(currencies),
                currencyCodes(currencies == null ? null : currencies.getAdditional()),
                countryCodes(raw.getShippingCountries()));
    }

    private static List<String> languageCodes(@Nullable List<MarketplaceItemLanguageRaw> languages) {
        return languages == null
                ? List.of()
                : languages.stream().map(MarketplaceItemLanguageRaw::getCode).toList();
    }

    private static @Nullable String baseCurrencyCode(@Nullable MarketplaceItemCurrenciesRaw currencies) {
        if (currencies == null || currencies.getBase() == null) {
            return null;
        }
        return currencies.getBase().getCode();
    }

    private static List<String> currencyCodes(@Nullable List<MarketplaceItemCurrencyRaw> currencies) {
        return currencies == null
                ? List.of()
                : currencies.stream().map(MarketplaceItemCurrencyRaw::getCode).toList();
    }

    private static List<String> countryCodes(@Nullable List<MarketplaceItemShippingCountryRaw> countries) {
        return countries == null
                ? List.of()
                : countries.stream().map(MarketplaceItemShippingCountryRaw::getCode).toList();
    }
}
