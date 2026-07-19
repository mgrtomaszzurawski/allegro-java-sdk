/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentPackage;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for one {@link ShipmentPackage}. The type, all three dimensions
 * (centimetres) and the weight (kilograms) are required; the label text is
 * optional. The waybill is assigned by the carrier and is not a build input.
 *
 * @since 0.4.0
 */
public final class ShipmentPackageBuilder {

    private static final String FIELD_TYPE = "ShipmentPackage.type";
    private static final String FIELD_LENGTH = "ShipmentPackage.lengthCm";
    private static final String FIELD_WIDTH = "ShipmentPackage.widthCm";
    private static final String FIELD_HEIGHT = "ShipmentPackage.heightCm";
    private static final String FIELD_WEIGHT = "ShipmentPackage.weightKg";

    private @Nullable PackageType type;
    private @Nullable BigDecimal lengthCm;
    private @Nullable BigDecimal widthCm;
    private @Nullable BigDecimal heightCm;
    private @Nullable BigDecimal weightKg;
    private @Nullable String textOnLabel;

    /** The parcel kind (required). */
    public ShipmentPackageBuilder type(@Nullable PackageType value) {
        this.type = value;
        return this;
    }

    /** The length in centimetres (required). */
    public ShipmentPackageBuilder lengthCm(@Nullable BigDecimal value) {
        this.lengthCm = value;
        return this;
    }

    /** The width in centimetres (required). */
    public ShipmentPackageBuilder widthCm(@Nullable BigDecimal value) {
        this.widthCm = value;
        return this;
    }

    /** The height in centimetres (required). */
    public ShipmentPackageBuilder heightCm(@Nullable BigDecimal value) {
        this.heightCm = value;
        return this;
    }

    /** The weight in kilograms (required). */
    public ShipmentPackageBuilder weightKg(@Nullable BigDecimal value) {
        this.weightKg = value;
        return this;
    }

    /** Free text printed on the label (optional). */
    public ShipmentPackageBuilder textOnLabel(@Nullable String value) {
        this.textOnLabel = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link ShipmentPackage}.
     *
     * @throws IllegalStateException if a required field is missing
     */
    public ShipmentPackage build() {
        PackageType validType = BuilderValidation.requirePresent(type, FIELD_TYPE);
        BigDecimal validLength = BuilderValidation.requirePresent(lengthCm, FIELD_LENGTH);
        BigDecimal validWidth = BuilderValidation.requirePresent(widthCm, FIELD_WIDTH);
        BigDecimal validHeight = BuilderValidation.requirePresent(heightCm, FIELD_HEIGHT);
        BigDecimal validWeight = BuilderValidation.requirePresent(weightKg, FIELD_WEIGHT);
        return new ShipmentPackage(validType, validLength, validWidth, validHeight,
                validWeight, textOnLabel, null);
    }
}
