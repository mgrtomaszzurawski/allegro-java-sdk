/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * OpenAPI-generated REST models ({@code *Raw} DTOs) for the Allegro REST API.
 * Implementation detail — exported only to allegro-client via a qualified
 * export. The {@code client.model} package is also opened to Jackson for
 * deserialization.
 */
module io.github.mgrtomaszzurawski.allegro.rest {

    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires transitive org.openapitools.jackson.nullable;
    requires transitive jakarta.annotation;
    // Supporting files: the native ApiClient uses the JDK HTTP client; composed
    // (oneOf/anyOf) DTOs log schema-resolution failures via java.util.logging.
    requires java.net.http;
    requires java.logging;

    // Invoker package (JSON, AbstractOpenApiSchema, ApiClient support classes)
    // and the DTO package — exported only to allegro-client.
    exports io.github.mgrtomaszzurawski.allegro.client to io.github.mgrtomaszzurawski.allegro;
    exports io.github.mgrtomaszzurawski.allegro.client.model to io.github.mgrtomaszzurawski.allegro;

    opens io.github.mgrtomaszzurawski.allegro.client.model to com.fasterxml.jackson.databind;
}
