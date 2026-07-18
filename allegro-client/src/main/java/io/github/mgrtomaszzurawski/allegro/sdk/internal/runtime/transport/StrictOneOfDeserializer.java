/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import io.github.mgrtomaszzurawski.allegro.client.model.AbstractOpenApiSchema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strict resolver for the generated {@code oneOf} wrappers ({@link AbstractOpenApiSchema}).
 *
 * <p>The generated deserializer trials each candidate schema with the client
 * ObjectMapper, which runs {@code FAIL_ON_UNKNOWN_PROPERTIES=false} for forward
 * compatibility — so a payload for one branch ALSO "matches" a sibling branch by
 * silently ignoring the properties it does not declare. The generator's own TODO
 * admits this: the match count is higher than it should be, and a structural
 * {@code oneOf} throws "2 classes match, expected 1".
 *
 * <p>This resolver re-trials each candidate with {@code FAIL_ON_UNKNOWN_PROPERTIES}
 * ENABLED, so a branch matches only when the payload carries no property foreign to
 * it — disambiguating structural {@code oneOf}s by which properties are present.
 * Exactly one strict match wins. If strict resolution is inconclusive (a
 * forward-compat extra field foreign to every branch, or none present) it falls back
 * to the lenient single-match, then to an {@code Object} catch-all branch — so an
 * ambiguous payload behaves no worse than the generated deserializer.
 *
 * <p>Installed via a mix-in ({@link StrictOneOfModule}) whose {@code @JsonDeserialize}
 * overrides the one the generator put on each wrapper. Contextualized per wrapper type.
 */
final class StrictOneOfDeserializer extends JsonDeserializer<Object>
        implements ContextualDeserializer {

    private static final Object NO_MATCH = new Object();

    private final Class<?> oneOfType;
    private final Map<String, Class<?>> candidateSchemas;

    StrictOneOfDeserializer() {
        this(null, Map.of());
    }

    private StrictOneOfDeserializer(Class<?> oneOfType, Map<String, Class<?>> candidateSchemas) {
        this.oneOfType = oneOfType;
        this.candidateSchemas = candidateSchemas;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        JavaType type = property != null ? property.getType() : context.getContextualType();
        Class<?> wrapper = type != null ? type.getRawClass() : null;
        return new StrictOneOfDeserializer(wrapper, StrictOneOfModule.candidateSchemasOf(wrapper));
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode tree = mapper.readTree(parser);

        Object strict = singleMatch(mapper, tree, true);
        if (strict != NO_MATCH) {
            return wrap(strict);
        }
        Object lenient = singleMatch(mapper, tree, false);
        if (lenient != NO_MATCH) {
            return wrap(lenient);
        }
        if (candidateSchemas.containsValue(Object.class)) {
            return wrap(mapper.convertValue(tree, Object.class));
        }
        throw new IOException("Cannot resolve oneOf " + oneOfType.getSimpleName()
                + ": no candidate schema matches the payload");
    }

    /** The sole matching typed candidate, or {@link #NO_MATCH} for zero or many. */
    private Object singleMatch(ObjectMapper mapper, JsonNode tree, boolean strict) {
        List<Object> matches = new ArrayList<>();
        for (Class<?> candidate : candidateSchemas.values()) {
            if (candidate == Object.class) {
                continue;
            }
            ObjectReader reader = strict
                    ? mapper.readerFor(candidate).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    : mapper.readerFor(candidate).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            Object value = readOrNull(reader, tree);
            if (value != null) {
                matches.add(value);
            }
        }
        return matches.size() == 1 ? matches.get(0) : NO_MATCH;
    }

    /** Deserialize {@code tree} with {@code reader}, or {@code null} when it does not match. */
    private static Object readOrNull(ObjectReader reader, JsonNode tree) {
        try {
            return reader.readValue(tree);
        } catch (IOException notThisBranch) {
            return null;
        }
    }

    private Object wrap(Object actualInstance) {
        try {
            AbstractOpenApiSchema wrapper =
                    (AbstractOpenApiSchema) oneOfType.getDeclaredConstructor().newInstance();
            wrapper.setActualInstance(actualInstance);
            return wrapper;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate oneOf wrapper " + oneOfType, e);
        }
    }
}
