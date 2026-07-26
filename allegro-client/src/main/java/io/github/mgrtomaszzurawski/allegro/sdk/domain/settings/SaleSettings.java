/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.AdditionalServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.Compliance;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.SizeTables;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model.TaxSettings;
import java.util.List;

/**
 * Seller sale settings — reached via {@code AllegroClient.settings()}.
 *
 * <p>Groups the seller-side configuration domains of bucket K (sale-settings):
 * after-sale service conditions, additional services, product-compliance
 * responsible parties, size tables and tax settings. Each is a nested
 * sub-facade; the bucket ships them incrementally per the task-division plan.
 *
 * @since 0.2.0
 */
public interface SaleSettings {

    /**
     * After-sale service conditions: warranties, implied warranties and return
     * policies (and their attachments).
     *
     * @return the after-sale conditions sub-facade
     */
    AfterSaleConditions afterSale();

    /**
     * Product-compliance (GPSR) responsible persons and producers.
     *
     * @return the compliance sub-facade
     */
    Compliance compliance();

    /**
     * Additional-services groups, their definition catalog and translations.
     *
     * @return the additional-services sub-facade
     */
    AdditionalServices additionalServices();

    /**
     * The seller's size tables and the templates they are built from.
     *
     * @return the size-tables sub-facade
     */
    SizeTables sizeTables();

    /**
     * The tax (VAT) options available for a category, across every country —
     * read-only reference data used when configuring an offer's invoice and VAT
     * settings.
     *
     * @param categoryId the category to read tax settings for
     * @return the category's tax settings
     */
    TaxSettings taxSettings(String categoryId);

    /**
     * The tax (VAT) options available for a category, narrowed to the given
     * countries. An empty {@code countryCodes} returns every country (same as
     * {@link #taxSettings(String)}).
     *
     * @param categoryId the category to read tax settings for
     * @param countryCodes the country codes to narrow to (e.g. {@code "PL"}, {@code "CZ"})
     * @return the category's tax settings for those countries
     */
    TaxSettings taxSettings(String categoryId, List<String> countryCodes);
}
