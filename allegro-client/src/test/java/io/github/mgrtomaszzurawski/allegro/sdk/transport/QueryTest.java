/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryTest {

    @Test
    void render_whenEmpty_returnsEmptyString() {
        // then — no parameters means no query string at all
        assertTrue(Query.create().isEmpty());
        assertEquals("", Query.create().render());
    }

    @Test
    void render_whenParams_buildsEncodedQueryString() {
        // when
        String rendered = Query.create()
                .add("name", "red shoes")
                .add("limit", 20)
                .render();

        // then — spaces encoded as %20, pairs joined with &, leading ?
        assertEquals("?name=red%20shoes&limit=20", rendered);
    }

    @Test
    void add_whenValueNull_omitsParameter() {
        // when — optional filter fields drop out rather than serialize as null
        String rendered = Query.create()
                .add("publication.status", "ACTIVE")
                .add("name", null)
                .render();

        // then
        assertEquals("?publication.status=ACTIVE", rendered);
    }

    @Test
    void addAll_whenRepeatedName_emitsOnePairPerValue() {
        // when — Allegro repeats parameters like offer.id
        String rendered = Query.create()
                .addAll("offer.id", List.of("11", "22"))
                .render();

        // then
        assertEquals("?offer.id=11&offer.id=22", rendered);
    }

    @Test
    void encode_whenReservedCharacters_arePercentEncoded() {
        // then — a value with & and = cannot break out into extra parameters
        assertEquals("?q=a%26b%3Dc", Query.create().add("q", "a&b=c").render());
    }
}
