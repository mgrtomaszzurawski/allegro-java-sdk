/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Marketing-campaign facades (bucket H) — reached via {@code AllegroClient.campaigns()}.
 * Groups Allegro's promotional programmes: badge campaigns ({@link
 * io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges}) and, as the
 * bucket grows, Allegro Prices and AlleDiscount. Methods are intent-named and
 * return immutable records from the {@code model} subpackage.
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;
