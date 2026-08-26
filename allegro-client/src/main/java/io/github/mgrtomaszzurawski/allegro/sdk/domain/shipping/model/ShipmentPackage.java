/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DimensionValueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PackageDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PackageRequestDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PackageTypeDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TransportingInfoDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WeightValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShipmentPackageBuilder;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One parcel in a shipment. Its kind and physical size drive how the carrier
 * handles and prices it: the SDK models dimensions in centimetres and weight in
 * kilograms — the only units Allegro's shipment API uses.
 *
 * <p>On a create request the type and all three dimensions plus the weight are
 * required; the label text is optional. On a read the waybill is filled in by
 * the carrier and the dimensions may be absent, so they are nullable.
 *
 * @param type the parcel kind
 * @param lengthCm the length in centimetres, or {@code null} on a read that omits it
 * @param widthCm the width in centimetres, or {@code null} on a read that omits it
 * @param heightCm the height in centimetres, or {@code null} on a read that omits it
 * @param weightKg the weight in kilograms, or {@code null} on a read that omits it
 * @param textOnLabel free text printed on the label, or {@code null}
 * @param waybill the carrier waybill number assigned on creation, or {@code null}
 * @param transportingInfo the per-carrier transporting info assigned on creation;
 *     never {@code null}, possibly empty (read-only)
 *
 * @since 0.4.0
 */
public record ShipmentPackage(
        PackageType type,
        @Nullable BigDecimal lengthCm,
        @Nullable BigDecimal widthCm,
        @Nullable BigDecimal heightCm,
        @Nullable BigDecimal weightKg,
        @Nullable String textOnLabel,
        @Nullable String waybill,
        List<ShipmentTransportingInfo> transportingInfo) {

    /** Canonical constructor — defensively copies the transporting-info list. */
    public ShipmentPackage {
        transportingInfo = transportingInfo == null ? List.of() : List.copyOf(transportingInfo);
    }

    /** A fresh builder for a {@link ShipmentPackage}. */
    public static ShipmentPackageBuilder builder() {
        return new ShipmentPackageBuilder();
    }

    /** A builder pre-loaded with this package's writable fields (waybill is read-only). */
    public ShipmentPackageBuilder toBuilder() {
        return new ShipmentPackageBuilder()
                .type(type)
                .lengthCm(lengthCm)
                .widthCm(widthCm)
                .heightCm(heightCm)
                .weightKg(weightKg)
                .textOnLabel(textOnLabel);
    }

    /** Map the generated response DTO to the public record. */
    public static ShipmentPackage from(PackageDtoRaw raw) {
        return new ShipmentPackage(
                PackageType.fromWire(raw.getType() == null ? null : raw.getType().getValue()),
                dimension(raw.getLength()),
                dimension(raw.getWidth()),
                dimension(raw.getHeight()),
                weight(raw.getWeight()),
                raw.getTextOnLabel(),
                raw.getWaybill(),
                transportingInfo(raw.getTransportingInfo()));
    }

    private static List<ShipmentTransportingInfo> transportingInfo(
            @Nullable List<TransportingInfoDtoRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(ShipmentTransportingInfo::from).toList();
    }

    /**
     * Map a generated request DTO (as echoed in a delivery proposal's suggested
     * input) to the public record. The waybill is a read-only carrier field and
     * is never present on a request DTO, so it is {@code null}.
     */
    public static ShipmentPackage fromRequest(PackageRequestDtoRaw raw) {
        return new ShipmentPackage(
                PackageType.fromWire(raw.getType() == null ? null : raw.getType().getValue()),
                dimension(raw.getLength()),
                dimension(raw.getWidth()),
                dimension(raw.getHeight()),
                weight(raw.getWeight()),
                raw.getTextOnLabel(),
                null,
                List.of());
    }

    /** Build the generated request DTO for a create body (writable fields only). */
    public PackageRequestDtoRaw toRaw() {
        PackageRequestDtoRaw raw = new PackageRequestDtoRaw();
        raw.setType(PackageTypeDtoRaw.fromValue(type.wireValue()));
        raw.setLength(dimensionRaw(lengthCm));
        raw.setWidth(dimensionRaw(widthCm));
        raw.setHeight(dimensionRaw(heightCm));
        raw.setWeight(weightRaw(weightKg));
        raw.setTextOnLabel(textOnLabel);
        return raw;
    }

    private static @Nullable BigDecimal dimension(@Nullable DimensionValueRaw raw) {
        return raw == null ? null : raw.getValue();
    }

    private static @Nullable BigDecimal weight(@Nullable WeightValueRaw raw) {
        return raw == null ? null : raw.getValue();
    }

    private static @Nullable DimensionValueRaw dimensionRaw(@Nullable BigDecimal centimetres) {
        if (centimetres == null) {
            return null;
        }
        DimensionValueRaw raw = new DimensionValueRaw();
        raw.setValue(centimetres);
        raw.setUnit(DimensionValueRaw.UnitEnum.CENTIMETER);
        return raw;
    }

    private static @Nullable WeightValueRaw weightRaw(@Nullable BigDecimal kilograms) {
        if (kilograms == null) {
            return null;
        }
        WeightValueRaw raw = new WeightValueRaw();
        raw.setValue(kilograms);
        raw.setUnit(WeightValueRaw.UnitEnum.KILOGRAMS);
        return raw;
    }
}
