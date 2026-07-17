/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Immutable value records returned by the {@code campaigns()} facade (bucket H):
 * badge campaigns, and — as the bucket grows — Allegro Prices and AlleDiscount
 * models. Each record is built from a generated Layer-1 {@code *Raw} DTO via a
 * {@code from(...)} factory and exposes only domain types to consumers.
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;
