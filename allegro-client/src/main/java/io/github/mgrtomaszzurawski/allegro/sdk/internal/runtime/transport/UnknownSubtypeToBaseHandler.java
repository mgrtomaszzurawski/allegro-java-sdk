/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Forward compatibility for discriminated polymorphic responses.
 *
 * <p>Generated {@code *Raw} bases carry {@code @JsonTypeInfo}/{@code @JsonSubTypes}
 * with NO {@code defaultImpl}, so a {@code type} discriminator this SDK release
 * does not model makes Jackson throw {@code InvalidTypeIdException} and fail the
 * WHOLE response before any domain mapping runs. This handler resolves an unknown
 * discriminator to the polymorphic BASE instead: the base keeps the raw {@code type}
 * string ({@code visible = true}) and its common fields, so a domain mapper lands
 * the instance on its own {@code OTHER}/unknown value rather than the read failing
 * (e.g. {@code CategoryParameterType.OTHER}).
 *
 * <p>Only a concrete base can be instantiated; an abstract base still fails (there
 * is nothing to construct). No Allegro polymorphic base modelled so far is abstract.
 */
public final class UnknownSubtypeToBaseHandler extends DeserializationProblemHandler {

    @Override
    public JavaType handleUnknownTypeId(DeserializationContext context, JavaType baseType,
            String subTypeId, TypeIdResolver idResolver, String failureMessage) {
        return baseType;
    }
}
