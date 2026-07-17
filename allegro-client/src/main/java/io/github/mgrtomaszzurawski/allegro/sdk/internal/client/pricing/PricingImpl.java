/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.Pricing;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Root implementation behind the {@link Pricing} facade. Holds the sub-facade
 * implementations and hands them out; each shares the same {@link HttpRuntime}.
 *
 * @since 0.2.0
 */
public final class PricingImpl implements Pricing {

    private final PricingAutomation automation;

    public PricingImpl(HttpRuntime runtime) {
        this.automation = new PricingAutomationImpl(runtime);
    }

    @Override
    public PricingAutomation automation() {
        return automation;
    }
}
