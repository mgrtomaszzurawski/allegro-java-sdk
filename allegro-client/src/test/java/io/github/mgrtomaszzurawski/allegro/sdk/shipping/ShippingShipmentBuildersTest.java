/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.CashOnDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelPageSize;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelSummaryField;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelSummaryPlacement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelSummaryReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast tests for the shipment-management builders (address,
 * package, cash-on-delivery, shipment request, label request and summary
 * report): every required field has a missing-field failure test and each
 * builder proves {@code toBuilder()} preserves its fields.
 */
class ShippingShipmentBuildersTest {

    private static final String STREET = "Grunwaldzka 100";
    private static final String POSTAL_CODE = "80-244";
    private static final String CITY = "Gdansk";
    private static final String EMAIL = "sender@example.com";
    private static final String PHONE = "+48500100100";
    private static final String STATE = "pomorskie";
    private static final String POINT = "POP-42";
    private static final String CURRENCY = "PLN";
    private static final String SHIPMENT_ID = "SHIP-1001";

    private static final BigDecimal LENGTH = new BigDecimal("30.0");
    private static final BigDecimal WIDTH = new BigDecimal("20.0");
    private static final BigDecimal HEIGHT = new BigDecimal("10.0");
    private static final BigDecimal WEIGHT = new BigDecimal("2.5");

    private static PostalAddress address() {
        return PostalAddress.builder()
                .street(STREET).postalCode(POSTAL_CODE).city(CITY)
                .email(EMAIL).phone(PHONE).build();
    }

    private static ShipmentPackage parcel() {
        return ShipmentPackage.builder()
                .type(PackageType.PACKAGE)
                .lengthCm(LENGTH).widthCm(WIDTH).heightCm(HEIGHT).weightKg(WEIGHT)
                .build();
    }

    // ---- PostalAddress ----

    @Test
    void addressBuilder_requiredFieldsOnly_leavesOptionalsNull() {
        PostalAddress built = address();
        assertEquals(STREET, built.street());
        assertEquals(CITY, built.city());
        assertNull(built.name());
        assertNull(built.state());
        assertNull(built.point());
    }

    @Test
    void addressBuilder_toBuilder_preservesFields() {
        PostalAddress original = address().toBuilder().state(STATE).point(POINT).build();
        PostalAddress copy = original.toBuilder().build();
        assertEquals(STATE, copy.state());
        assertEquals(POINT, copy.point());
        assertEquals(PHONE, copy.phone());
    }

    @Test
    void addressBuilder_whenStreetMissing_throws() {
        assertMessage("PostalAddress.street is required", assertThrows(IllegalStateException.class,
                () -> PostalAddress.builder().postalCode(POSTAL_CODE).city(CITY)
                        .email(EMAIL).phone(PHONE).build()));
    }

    @Test
    void addressBuilder_whenPostalCodeMissing_throws() {
        assertMessage("PostalAddress.postalCode is required", assertThrows(IllegalStateException.class,
                () -> PostalAddress.builder().street(STREET).city(CITY)
                        .email(EMAIL).phone(PHONE).build()));
    }

    @Test
    void addressBuilder_whenCityMissing_throws() {
        assertMessage("PostalAddress.city is required", assertThrows(IllegalStateException.class,
                () -> PostalAddress.builder().street(STREET).postalCode(POSTAL_CODE)
                        .email(EMAIL).phone(PHONE).build()));
    }

    @Test
    void addressBuilder_whenEmailMissing_throws() {
        assertMessage("PostalAddress.email is required", assertThrows(IllegalStateException.class,
                () -> PostalAddress.builder().street(STREET).postalCode(POSTAL_CODE).city(CITY)
                        .phone(PHONE).build()));
    }

    @Test
    void addressBuilder_whenPhoneMissing_throws() {
        assertMessage("PostalAddress.phone is required", assertThrows(IllegalStateException.class,
                () -> PostalAddress.builder().street(STREET).postalCode(POSTAL_CODE).city(CITY)
                        .email(EMAIL).build()));
    }

    // ---- ShipmentPackage ----

    @Test
    void packageBuilder_allFieldsSet_builds() {
        ShipmentPackage built = parcel().toBuilder().textOnLabel("fragile").build();
        assertEquals(PackageType.PACKAGE, built.type());
        assertEquals(LENGTH, built.lengthCm());
        assertEquals(WEIGHT, built.weightKg());
        assertEquals("fragile", built.textOnLabel());
        assertNull(built.waybill());
    }

    @Test
    void packageBuilder_whenTypeMissing_throws() {
        assertMessage("ShipmentPackage.type is required", assertThrows(IllegalStateException.class,
                () -> ShipmentPackage.builder().lengthCm(LENGTH).widthCm(WIDTH)
                        .heightCm(HEIGHT).weightKg(WEIGHT).build()));
    }

    @Test
    void packageBuilder_whenLengthMissing_throws() {
        assertMessage("ShipmentPackage.lengthCm is required", assertThrows(IllegalStateException.class,
                () -> ShipmentPackage.builder().type(PackageType.PACKAGE).widthCm(WIDTH)
                        .heightCm(HEIGHT).weightKg(WEIGHT).build()));
    }

    @Test
    void packageBuilder_whenWidthMissing_throws() {
        assertMessage("ShipmentPackage.widthCm is required", assertThrows(IllegalStateException.class,
                () -> ShipmentPackage.builder().type(PackageType.PACKAGE).lengthCm(LENGTH)
                        .heightCm(HEIGHT).weightKg(WEIGHT).build()));
    }

    @Test
    void packageBuilder_whenHeightMissing_throws() {
        assertMessage("ShipmentPackage.heightCm is required", assertThrows(IllegalStateException.class,
                () -> ShipmentPackage.builder().type(PackageType.PACKAGE).lengthCm(LENGTH)
                        .widthCm(WIDTH).weightKg(WEIGHT).build()));
    }

    @Test
    void packageBuilder_whenWeightMissing_throws() {
        assertMessage("ShipmentPackage.weightKg is required", assertThrows(IllegalStateException.class,
                () -> ShipmentPackage.builder().type(PackageType.PACKAGE).lengthCm(LENGTH)
                        .widthCm(WIDTH).heightCm(HEIGHT).build()));
    }

    // ---- CashOnDelivery ----

    @Test
    void cashOnDeliveryBuilder_allFieldsSet_builds() {
        CashOnDelivery built = CashOnDelivery.builder()
                .amount(Money.of("250.00", CURRENCY)).ownerName("Seller").iban("PL61").build();
        assertEquals(Money.of("250.00", CURRENCY), built.amount());
        assertEquals("PL61", built.iban());
    }

    @Test
    void cashOnDeliveryBuilder_whenAmountMissing_throws() {
        assertMessage("CashOnDelivery.amount is required", assertThrows(IllegalStateException.class,
                () -> CashOnDelivery.builder().ownerName("Seller").build()));
    }

    @Test
    void cashOnDelivery_toString_redactsIban() {
        CashOnDelivery built = CashOnDelivery.builder()
                .amount(Money.of("250.00", CURRENCY)).ownerName("Seller Ltd")
                .iban("PL61109010140000071219812874").build();
        String rendered = built.toString();
        assertFalse(rendered.contains("PL61109010140000071219812874"));
        assertFalse(rendered.contains("Seller Ltd"));
    }

    // ---- ShipmentRequest ----

    @Test
    void shipmentRequestBuilder_requiredFieldsOnly_leavesOptionalsNull() {
        ShipmentRequest built = ShipmentRequest.builder()
                .sender(address()).receiver(address()).packages(List.of(parcel())).build();
        assertEquals(1, built.packages().size());
        assertNull(built.credentialsId());
        assertNull(built.insurance());
        assertNull(built.labelFormat());
    }

    @Test
    void shipmentRequestBuilder_toBuilder_preservesFields() {
        ShipmentRequest original = ShipmentRequest.builder()
                .sender(address()).receiver(address()).packages(List.of(parcel()))
                .credentialsId("CRED-1").insurance(Money.of("100.00", CURRENCY))
                .labelFormat(LabelFormat.ZPL).build();
        ShipmentRequest copy = original.toBuilder().build();
        assertEquals("CRED-1", copy.credentialsId());
        assertEquals(Money.of("100.00", CURRENCY), copy.insurance());
        assertEquals(LabelFormat.ZPL, copy.labelFormat());
    }

    @Test
    void shipmentRequestBuilder_whenSenderMissing_throws() {
        assertMessage("ShipmentRequest.sender is required", assertThrows(IllegalStateException.class,
                () -> ShipmentRequest.builder().receiver(address())
                        .packages(List.of(parcel())).build()));
    }

    @Test
    void shipmentRequestBuilder_whenReceiverMissing_throws() {
        assertMessage("ShipmentRequest.receiver is required", assertThrows(IllegalStateException.class,
                () -> ShipmentRequest.builder().sender(address())
                        .packages(List.of(parcel())).build()));
    }

    @Test
    void shipmentRequestBuilder_whenPackagesMissing_throws() {
        assertMessage("ShipmentRequest.packages is required", assertThrows(IllegalStateException.class,
                () -> ShipmentRequest.builder().sender(address()).receiver(address()).build()));
    }

    @Test
    void shipmentRequestBuilder_whenPackagesEmpty_throws() {
        assertMessage("ShipmentRequest.packages is required", assertThrows(IllegalStateException.class,
                () -> ShipmentRequest.builder().sender(address()).receiver(address())
                        .packages(List.of()).build()));
    }

    // ---- LabelRequest ----

    @Test
    void labelRequestBuilder_requiredOnly_leavesOptionalsNull() {
        LabelRequest built = LabelRequest.builder().shipmentIds(List.of(SHIPMENT_ID)).build();
        assertEquals(1, built.shipmentIds().size());
        assertNull(built.pageSize());
        assertNull(built.cutLine());
        assertNull(built.summaryReport());
    }

    @Test
    void labelRequestBuilder_allFieldsSet_builds() {
        LabelRequest built = LabelRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID))
                .pageSize(LabelPageSize.A6)
                .cutLine(Boolean.TRUE)
                .summaryReport(LabelSummaryReport.of(LabelSummaryPlacement.LAST,
                        List.of(LabelSummaryField.WAYBILL)))
                .build();
        assertEquals(LabelPageSize.A6, built.pageSize());
        assertTrue(built.cutLine());
        assertEquals(LabelSummaryPlacement.LAST, built.summaryReport().placement());
    }

    @Test
    void labelRequestBuilder_whenShipmentIdsMissing_throws() {
        assertMessage("LabelRequest.shipmentIds is required", assertThrows(IllegalStateException.class,
                () -> LabelRequest.builder().pageSize(LabelPageSize.A4).build()));
    }

    @Test
    void labelRequestBuilder_whenShipmentIdsEmpty_throws() {
        assertMessage("LabelRequest.shipmentIds is required", assertThrows(IllegalStateException.class,
                () -> LabelRequest.builder().shipmentIds(List.of()).build()));
    }

    // ---- LabelSummaryReport ----

    @Test
    void summaryReport_whenFieldsEmpty_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> LabelSummaryReport.of(LabelSummaryPlacement.EVERY, List.of()));
    }

    @Test
    void summaryReport_whenPlacementNull_throws() {
        assertThrows(NullPointerException.class,
                () -> LabelSummaryReport.of(null, List.of(LabelSummaryField.COD)));
    }

    private static void assertMessage(String expected, IllegalStateException actual) {
        assertEquals(expected, actual.getMessage());
    }
}
