/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Typed Java SDK for the Allegro REST API.
 *
 * <p>Bootstrap scaffold: only the entry-point package is exported so far.
 * Domain packages ({@code sdk.config}, {@code sdk.config.credentials},
 * {@code sdk.config.policy}, {@code sdk.core}, {@code sdk.exception},
 * {@code sdk.domain.*}) are added with their owning PRs once the task-division
 * plan is accepted — each domain PR adds its {@code exports} lines here.
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
    // Remediation-grouped exception hierarchy.
    exports io.github.mgrtomaszzurawski.allegro.sdk.exception;

    // [append point: domain exports] Each domain bucket appends its
    // `exports ...sdk.domain.<feature>...` lines below this marker, one block
    // per bucket, in BACKLOG order.

    // Account and user information (bucket D; me() ships with the core PR).
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.account;
    exports io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

    // Generated *Raw DTOs (Layer 1) — internal use only, not re-exported.
    requires io.github.mgrtomaszzurawski.allegro.rest;

    // Transport + JSON mapping.
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;

    // JSpecify null-safety annotations (compile-time only).
    requires static org.jspecify;

    // apiguardian @API EXPERIMENTAL marker on AllegroClient (preview release).
    // transitive so consumers reading the RUNTIME-retained annotation resolve
    // it without declaring apiguardian themselves; static keeps it optional
    // at run time.
    requires static transitive org.apiguardian.api;
}
