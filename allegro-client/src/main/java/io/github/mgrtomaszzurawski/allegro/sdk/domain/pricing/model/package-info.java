/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Immutable request and response records for the pricing facade. Request records
 * are assembled by the fluent builders in the sibling {@code builder} package.
 * Response records are mapped from the wire by the package-private
 * {@code internal.client.pricing.PricingMapper}; the rule response is mapped from
 * a {@code JsonNode} rather than a {@code from(Raw)} factory because the generated
 * {@code native} oneOf deserializer for the configuration over-matches (see
 * {@code PricingMapper}).
 *
 * @since 0.2.0
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;
