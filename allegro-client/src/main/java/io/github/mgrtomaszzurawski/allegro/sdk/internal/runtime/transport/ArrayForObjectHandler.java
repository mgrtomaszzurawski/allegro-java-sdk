/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import java.io.IOException;

/**
 * Forward compatibility for fields where Allegro returns a JSON array but the
 * vendored OpenAPI spec types the field as a single object.
 *
 * <p>The generated {@code *Raw} DTOs mirror the spec, so such a field is a scalar
 * bean property; the real API returning {@code [ ... ]} makes Jackson throw
 * {@code MismatchedInputException} ("Cannot deserialize value of type X from Array
 * value") and fail the WHOLE response before any domain mapping runs. This was
 * observed live on {@code SaleProductOfferResponseV1Raw.warnings} (spec: object,
 * server: array — always {@code []} on a valid offer), which alone broke every
 * {@code getProductOffer} read.
 *
 * <p>Rather than fail the read, this handler consumes the unexpected array: an empty
 * array becomes {@code null}, a non-empty one becomes its FIRST element deserialized
 * as the expected type (later elements are dropped). Dropping is acceptable because
 * the field is, by the spec, single-valued and the SDK's domain models do not surface
 * it; the alternative — failing the entire response — is strictly worse.
 *
 * <p>Only triggers when the expected type is a single value (not a collection/array),
 * so a legitimately array-typed field is never touched. Mirrors
 * {@link UnknownSubtypeToBaseHandler}: a narrow transport-boundary tolerance that keeps
 * a spec/runtime drift from failing an otherwise-good response.
 */
public final class ArrayForObjectHandler extends DeserializationProblemHandler {

    @Override
    public Object handleUnexpectedToken(DeserializationContext context, JavaType targetType,
            JsonToken token, JsonParser parser, String failureMessage) throws IOException {
        if (token == JsonToken.START_ARRAY && !targetType.isContainerType() && !targetType.isArrayType()) {
            Object first = null;
            boolean taken = false;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (taken) {
                    parser.skipChildren();
                } else {
                    first = context.readValue(parser, targetType);
                    taken = true;
                }
            }
            return first;
        }
        return super.handleUnexpectedToken(context, targetType, token, parser, failureMessage);
    }
}
