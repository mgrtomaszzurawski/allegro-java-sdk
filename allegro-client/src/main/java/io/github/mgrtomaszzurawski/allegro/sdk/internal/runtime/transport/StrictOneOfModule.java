/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.mgrtomaszzurawski.allegro.client.model.AbstractOpenApiSchema;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.BadgePatchRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAutomaticPricingCommandModificationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoActualPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoDeclaredPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoRecommendedPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationRequestSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationResponseSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductSafetyDtoSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaskWithSubjectReportTasksInnerRaw;
import java.util.Map;

/**
 * Installs {@link StrictOneOfDeserializer} on every generated {@code oneOf} wrapper
 * so an ambiguous {@code oneOf} response is resolved by strict property matching
 * instead of failing with "N classes match, expected 1" (the generator's known
 * over-match — see the deserializer's Javadoc).
 *
 * <p>The generator annotates each wrapper with its own {@code @JsonDeserialize},
 * which a {@code BeanDeserializerModifier} cannot override. A mix-in CAN: a mix-in's
 * annotations take precedence over the target class's, so {@link #STRICT_ONE_OF_TYPES}
 * are re-pointed at the strict deserializer.
 *
 * <p>New {@code oneOf} wrapper types added by a future spec regeneration must be added
 * to {@link #STRICT_ONE_OF_TYPES} (they otherwise keep the over-matching generated
 * deserializer).
 */
public final class StrictOneOfModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    private static final Class<?>[] STRICT_ONE_OF_TYPES = {
        AutomaticPricingRuleConfigurationRaw.class,
        BadgePatchRequestRaw.class,
        CompatibilityListItemRaw.class,
        CompatibilityListProductOfferResponseRaw.class,
        OfferAutomaticPricingCommandModificationRaw.class,
        OfferStatusItemDtoActualPriceReductionRaw.class,
        OfferStatusItemDtoDeclaredPriceReductionRaw.class,
        OfferStatusItemDtoRecommendedPriceReductionRaw.class,
        ProductSetElementSafetyInformationRequestSafetyInformationRaw.class,
        ProductSetElementSafetyInformationResponseSafetyInformationRaw.class,
        SaleProductSafetyDtoSafetyInformationRaw.class,
        TaskWithSubjectReportTasksInnerRaw.class,
    };

    public StrictOneOfModule() {
        super("StrictOneOfModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        for (Class<?> oneOfType : STRICT_ONE_OF_TYPES) {
            context.setMixInAnnotations(oneOfType, StrictOneOfMixIn.class);
        }
    }

    /** Read a oneOf wrapper's candidate schemas from a throwaway instance. */
    static Map<String, Class<?>> candidateSchemasOf(Class<?> oneOfType) {
        if (oneOfType == null || !AbstractOpenApiSchema.class.isAssignableFrom(oneOfType)) {
            return Map.of();
        }
        try {
            AbstractOpenApiSchema probe =
                    (AbstractOpenApiSchema) oneOfType.getDeclaredConstructor().newInstance();
            return probe.getSchemas();
        } catch (ReflectiveOperationException notAStandardWrapper) {
            return Map.of();
        }
    }

    /** Mix-in whose {@code @JsonDeserialize} overrides the generator's on each wrapper. */
    @JsonDeserialize(using = StrictOneOfDeserializer.class)
    private interface StrictOneOfMixIn {
    }
}
