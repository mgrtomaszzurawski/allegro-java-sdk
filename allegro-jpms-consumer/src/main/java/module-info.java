/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * JPMS consumer compile gate — confirms the SDK's public surface is fully
 * resolvable from a downstream named module with no {@code --add-exports}
 * hacks. If a change moves a public type into a non-exported package, this
 * module stops compiling.
 */
module io.github.mgrtomaszzurawski.allegro.jpms.consumer {
    requires io.github.mgrtomaszzurawski.allegro;
}
