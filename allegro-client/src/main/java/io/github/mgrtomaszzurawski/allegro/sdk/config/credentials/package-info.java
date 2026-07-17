/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Sealed OAuth2 credential hierarchy — one implementation per grant type (client-credentials, authorization-code, device). The consumer authenticates once; token lifecycle is SDK-internal.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;

import org.jspecify.annotations.NullMarked;
