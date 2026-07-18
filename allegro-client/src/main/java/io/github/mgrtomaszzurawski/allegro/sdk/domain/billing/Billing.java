/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.billing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder.BillingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingType;
import java.util.List;
import java.util.stream.Stream;

/**
 * The seller's billing ledger — reached via {@code AllegroClient.billing()}.
 *
 * @since 0.5.0
 */
public interface Billing {

    /**
     * Lazily stream the seller's billing entries matching {@code filter}, pages
     * fetched on demand.
     *
     * @param filter the billing filter ({@link BillingFilter#all()} for every entry)
     * @return a lazy stream of billing entries
     */
    Stream<BillingEntry> streamEntries(BillingFilter filter);

    /**
     * List the billing-type dictionary (id + description of each charge kind).
     *
     * @return the billing types; never {@code null}
     */
    List<BillingType> types();
}
