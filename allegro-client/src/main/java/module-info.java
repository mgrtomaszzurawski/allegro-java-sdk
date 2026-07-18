/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Typed Java SDK for the Allegro REST API. Exports the {@code sdk.*} public
 * surface (entry point, configuration, credentials, policy, exceptions, and
 * the domain facades); everything under {@code sdk.internal.*} and the
 * generated {@code *Raw} DTO module stay invisible to consumers. Domain
 * buckets append their {@code exports} lines at the marked append point.
 */
module io.github.mgrtomaszzurawski.allegro {

    // SDK public API — entry point.
    exports io.github.mgrtomaszzurawski.allegro.sdk;

    // Configuration: environment + client options.
    exports io.github.mgrtomaszzurawski.allegro.sdk.config;
    // Sealed OAuth2 credential hierarchy (three grant types).
    exports io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;
    // Policy knobs: retry.
    exports io.github.mgrtomaszzurawski.allegro.sdk.config.policy;
    // Shared cross-bucket value types (Money, ...).
    exports io.github.mgrtomaszzurawski.allegro.sdk.core;
    // Remediation-grouped exception hierarchy.
    exports io.github.mgrtomaszzurawski.allegro.sdk.exception;

    // [append point: domain exports] Each domain bucket appends its
    // `exports ...sdk.domain.<feature>...` lines below this marker, one block
    // per bucket, in BACKLOG order.

    // Orders, payments and billing (bucket B).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.payments;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.billing;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model;

    // Shipping facade (bucket C): shipments, delivery, points of service.
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

    // Sale settings — seller-side configuration (bucket K). Starter slice:
    // after-sale service conditions (warranties).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.settings;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder;

    // Account and user information (bucket D; me() ships with the core PR).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.account;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder;

    // Offer lifecycle (bucket A).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

    // Product catalogue: categories, products, compatibility (bucket E).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

    // Classifieds (advertisement) packages and statistics (bucket F).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

    // Pricing: automatic pricing rules, promotions, deposits (bucket G).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;
    // Marketing campaigns: badges, Allegro Prices, AlleDiscount (bucket H).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

    // One Fulfillment by Allegro (bucket I).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder;

    // Seller contact cards (bucket J — post-sale-comms).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model;

    // Message center (bucket J — post-sale-comms).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

    // Post-purchase issues: disputes and claims (bucket J — post-sale-comms).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model;

    // Generated *Raw DTOs (Layer 1) — internal use only, not re-exported.
    requires io.github.mgrtomaszzurawski.allegro.rest;

    // Transport + JSON mapping. jackson.nullable is used directly (the SDK
    // registers JsonNullableModule), so the dependence is declared here, not
    // inherited from the generated module's transitivity.
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.openapitools.jackson.nullable;
    requires org.slf4j;

    // JSpecify null-safety annotations — static (compile-time only, absent at
    // run time; consumers wanting to READ the annotations add jspecify
    // themselves - transitive would force it onto every consumer's module
    // path at compile time).
    requires static org.jspecify;

    // apiguardian @API EXPERIMENTAL marker on AllegroClient (preview release).
    // transitive so consumers reading the RUNTIME-retained annotation resolve
    // it without declaring apiguardian themselves; static keeps it optional
    // at run time.
    requires static transitive org.apiguardian.api;
}
