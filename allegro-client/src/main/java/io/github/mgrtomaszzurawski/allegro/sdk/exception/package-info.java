/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Unchecked exception hierarchy grouped by remediation (fix request / fix credentials / slow down / retry later / fix configuration), with typed field-level errors parsed from Allegro's {@code errors[]} payload.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import org.jspecify.annotations.NullMarked;
