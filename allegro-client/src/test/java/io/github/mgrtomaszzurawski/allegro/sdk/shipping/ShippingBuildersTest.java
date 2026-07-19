/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.PointOfServiceRequestBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShippingRateBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Address;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Coordinates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.JoinStrategy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Weight;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast tests for the shipping builders: every required
 * field has a missing-field failure test, every length limit has an over-limit
 * test, and each builder proves {@code toBuilder()} preserves its fields.
 */
class ShippingBuildersTest {

    private static final String CITY = "Gdansk";
    private static final String ZIP_CODE = "80-244";
    private static final String STATE = "pomorskie";
    private static final String COUNTRY_CODE = "PL";
    private static final String STREET = "Grunwaldzka 100";
    private static final String NAME = "Pickup Point Center";
    private static final String EXTERNAL_ID = "agent-c-demo-001";
    private static final String PHONE = "+48111222333";
    private static final String EMAIL = "pickup@example.com";
    private static final String DAY_OF_WEEK = "MONDAY";
    private static final String OPEN_FROM = "08:00";
    private static final String OPEN_TO = "16:00";
    private static final String FILLER = "x";

    private static final int MAX_NAME = 80;
    private static final int MAX_EXTERNAL_ID = 80;
    private static final int MAX_PHONE = 16;
    private static final int MAX_EMAIL = 64;
    private static final int MAX_STREET = 80;
    private static final int MAX_CITY = 40;
    private static final int MAX_ZIP_CODE = 10;
    private static final int MAX_STATE = 40;

    private static Address fullAddress() {
        return Address.builder()
                .street(STREET).city(CITY).zipCode(ZIP_CODE).state(STATE)
                .countryCode(COUNTRY_CODE).coordinates(new Coordinates(54.3, 18.6)).build();
    }

    private static OpenHour openHour() {
        return OpenHour.builder().dayOfWeek(DAY_OF_WEEK).fromTime(OPEN_FROM).toTime(OPEN_TO).build();
    }

    private static String tooLong(int maxLength) {
        return FILLER.repeat(maxLength + 1);
    }

    // ---- AddressBuilder ----

    @Test
    void addressBuilder_requiredFieldsOnly_buildsWithNullStreet() {
        // when — street is the only optional field; coordinates are required
        Address address = Address.builder()
                .city(CITY).zipCode(ZIP_CODE).state(STATE).countryCode(COUNTRY_CODE)
                .coordinates(new Coordinates(54.3, 18.6)).build();

        // then
        assertNull(address.street());
        assertEquals(CITY, address.city());
    }

    @Test
    void addressBuilder_whenCoordinatesMissing_throws() {
        var builder = Address.builder().city(CITY).zipCode(ZIP_CODE).state(STATE)
                .countryCode(COUNTRY_CODE);
        assertMessage("Address.coordinates is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_allCoreFieldsSet_buildsFullAddress() {
        // when
        Address address = fullAddress();

        // then
        assertEquals(STREET, address.street());
        assertEquals(ZIP_CODE, address.zipCode());
        assertEquals(STATE, address.state());
        assertEquals(COUNTRY_CODE, address.countryCode());
        assertEquals(18.6, address.coordinates().longitude());
    }

    @Test
    void addressBuilder_toBuilder_preservesFields() {
        // given
        Address original = fullAddress();

        // when
        Address copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void addressBuilder_whenCityMissing_throws() {
        var builder = Address.builder().zipCode(ZIP_CODE).state(STATE).countryCode(COUNTRY_CODE);
        assertMessage("Address.city is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenZipCodeMissing_throws() {
        var builder = Address.builder().city(CITY).state(STATE).countryCode(COUNTRY_CODE);
        assertMessage("Address.zipCode is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenStateMissing_throws() {
        var builder = Address.builder().city(CITY).zipCode(ZIP_CODE).countryCode(COUNTRY_CODE);
        assertMessage("Address.state is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenCountryCodeMissing_throws() {
        var builder = Address.builder().city(CITY).zipCode(ZIP_CODE).state(STATE);
        assertMessage("Address.countryCode is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenCityTooLong_throws() {
        var builder = Address.builder()
                .city(tooLong(MAX_CITY)).zipCode(ZIP_CODE).state(STATE).countryCode(COUNTRY_CODE);
        assertMessage("Address.city must be at most 40 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenStreetTooLong_throws() {
        var builder = Address.builder().street(tooLong(MAX_STREET))
                .city(CITY).zipCode(ZIP_CODE).state(STATE).countryCode(COUNTRY_CODE);
        assertMessage("Address.street must be at most 80 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenZipCodeTooLong_throws() {
        var builder = Address.builder()
                .city(CITY).zipCode(tooLong(MAX_ZIP_CODE)).state(STATE).countryCode(COUNTRY_CODE);
        assertMessage("Address.zipCode must be at most 10 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void addressBuilder_whenStateTooLong_throws() {
        var builder = Address.builder()
                .city(CITY).zipCode(ZIP_CODE).state(tooLong(MAX_STATE)).countryCode(COUNTRY_CODE);
        assertMessage("Address.state must be at most 40 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    // ---- OpenHourBuilder ----

    @Test
    void openHourBuilder_allFieldsSet_builds() {
        // when
        OpenHour hour = openHour();

        // then
        assertEquals(DAY_OF_WEEK, hour.dayOfWeek());
        assertEquals(OPEN_FROM, hour.fromTime());
        assertEquals(OPEN_TO, hour.toTime());
    }

    @Test
    void openHourBuilder_toBuilder_preservesFields() {
        // given
        OpenHour original = openHour();

        // when
        OpenHour copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void openHourBuilder_whenDayOfWeekMissing_throws() {
        var builder = OpenHour.builder().fromTime(OPEN_FROM).toTime(OPEN_TO);
        assertMessage("OpenHour.dayOfWeek is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void openHourBuilder_whenFromTimeMissing_throws() {
        var builder = OpenHour.builder().dayOfWeek(DAY_OF_WEEK).toTime(OPEN_TO);
        assertMessage("OpenHour.fromTime is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void openHourBuilder_whenToTimeMissing_throws() {
        var builder = OpenHour.builder().dayOfWeek(DAY_OF_WEEK).fromTime(OPEN_FROM);
        assertMessage("OpenHour.toTime is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    // ---- PointOfServiceRequestBuilder ----

    private static PointOfServiceRequestBuilder minimalRequest() {
        return PointOfServiceRequest.builder()
                .name(NAME)
                .type(PosType.PICKUP_POINT)
                .status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
                .address(fullAddress());
    }

    @Test
    void requestBuilder_requiredFieldsOnly_buildsWithEmptyOpenHours() {
        // when
        PointOfServiceRequest request = minimalRequest().build();

        // then — openHours defaults to an empty (never null) list
        assertTrue(request.openHours().isEmpty());
        assertNull(request.externalId());
        assertEquals(NAME, request.name());
    }

    @Test
    void requestBuilder_allCoreFieldsSet_builds() {
        // when
        PointOfServiceRequest request = minimalRequest()
                .openHours(List.of(openHour()))
                .externalId(EXTERNAL_ID)
                .phoneNumber(PHONE)
                .email(EMAIL)
                .serviceTime("PT24H")
                .build();

        // then
        assertEquals(PosType.PICKUP_POINT, request.type());
        assertEquals(PosStatus.ACTIVE, request.status());
        assertEquals(ConfirmationType.CONTACT_NOT_REQUIRED, request.confirmationType());
        assertEquals(EXTERNAL_ID, request.externalId());
        assertEquals(PHONE, request.phoneNumber());
        assertEquals(EMAIL, request.email());
        assertEquals("PT24H", request.serviceTime());
        assertEquals(1, request.openHours().size());
    }

    @Test
    void requestBuilder_toBuilder_preservesFields() {
        // given
        PointOfServiceRequest original = minimalRequest()
                .openHours(List.of(openHour())).externalId(EXTERNAL_ID).build();

        // when
        PointOfServiceRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void requestBuilder_whenNameMissing_throws() {
        var builder = PointOfServiceRequest.builder()
                .type(PosType.PICKUP_POINT).status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED).address(fullAddress());
        assertMessage("PointOfServiceRequest.name is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenTypeMissing_throws() {
        var builder = PointOfServiceRequest.builder()
                .name(NAME).status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED).address(fullAddress());
        assertMessage("PointOfServiceRequest.type is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenStatusMissing_throws() {
        var builder = PointOfServiceRequest.builder()
                .name(NAME).type(PosType.PICKUP_POINT)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED).address(fullAddress());
        assertMessage("PointOfServiceRequest.status is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenConfirmationTypeMissing_throws() {
        var builder = PointOfServiceRequest.builder()
                .name(NAME).type(PosType.PICKUP_POINT).status(PosStatus.ACTIVE)
                .address(fullAddress());
        assertMessage("PointOfServiceRequest.confirmationType is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenAddressMissing_throws() {
        var builder = PointOfServiceRequest.builder()
                .name(NAME).type(PosType.PICKUP_POINT).status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED);
        assertMessage("PointOfServiceRequest.address is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenNameTooLong_throws() {
        var builder = minimalRequest().name(tooLong(MAX_NAME));
        assertMessage("PointOfServiceRequest.name must be at most 80 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenExternalIdTooLong_throws() {
        var builder = minimalRequest().externalId(tooLong(MAX_EXTERNAL_ID));
        assertMessage("PointOfServiceRequest.externalId must be at most 80 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenPhoneNumberTooLong_throws() {
        var builder = minimalRequest().phoneNumber(tooLong(MAX_PHONE));
        assertMessage("PointOfServiceRequest.phoneNumber must be at most 16 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void requestBuilder_whenEmailTooLong_throws() {
        var builder = minimalRequest().email(tooLong(MAX_EMAIL));
        assertMessage("PointOfServiceRequest.email must be at most 64 characters",
                assertThrows(IllegalStateException.class, builder::build));
    }

    // ---- DeliverySettingsRequestBuilder ----

    @Test
    void deliverySettingsBuilder_requiredOnly_buildsWithNullOptionals() {
        // when — joinPolicy is the only required field
        DeliverySettingsRequest request = DeliverySettingsRequest.builder()
                .joinPolicy(JoinStrategy.SUM).build();

        // then
        assertEquals(JoinStrategy.SUM, request.joinPolicy());
        assertNull(request.marketplaceId());
        assertNull(request.freeDelivery());
        assertNull(request.abroadFreeDelivery());
    }

    @Test
    void deliverySettingsBuilder_allFieldsSet_builds() {
        // when
        DeliverySettingsRequest request = DeliverySettingsRequest.builder()
                .marketplaceId(COUNTRY_CODE)
                .freeDelivery(Money.of("200.00", "PLN"))
                .abroadFreeDelivery(Money.of("500.00", "PLN"))
                .joinPolicy(JoinStrategy.MAX)
                .build();

        // then
        assertEquals(COUNTRY_CODE, request.marketplaceId());
        assertEquals(Money.of("200.00", "PLN"), request.freeDelivery());
        assertEquals(Money.of("500.00", "PLN"), request.abroadFreeDelivery());
        assertEquals(JoinStrategy.MAX, request.joinPolicy());
    }

    @Test
    void deliverySettingsBuilder_toBuilder_preservesFields() {
        // given
        DeliverySettingsRequest original = DeliverySettingsRequest.builder()
                .marketplaceId(COUNTRY_CODE)
                .freeDelivery(Money.of("200.00", "PLN"))
                .joinPolicy(JoinStrategy.MIN)
                .build();

        // when
        DeliverySettingsRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void deliverySettingsBuilder_whenJoinPolicyMissing_throws() {
        var builder = DeliverySettingsRequest.builder().freeDelivery(Money.of("200.00", "PLN"));
        assertMessage("DeliverySettingsRequest.joinPolicy is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    // ---- ShippingRateBuilder ----

    private static final String METHOD_ID = "method-courier";

    private static ShippingRateBuilder minimalRate() {
        return ShippingRate.builder()
                .deliveryMethodId(METHOD_ID)
                .firstItemRate(Money.of("12.99", "PLN"))
                .nextItemRate(Money.of("2.00", "PLN"))
                .maxQuantityPerPackage(10);
    }

    @Test
    void rateBuilder_requiredFieldsOnly_buildsWithNullOptionals() {
        // when
        ShippingRate rate = minimalRate().build();

        // then
        assertEquals(METHOD_ID, rate.deliveryMethodId());
        assertNull(rate.maxPackageWeight());
        assertNull(rate.shippingTime());
    }

    @Test
    void rateBuilder_allFieldsSet_builds() {
        // when
        ShippingRate rate = minimalRate()
                .maxPackageWeight(new Weight("30.0", "KILOGRAMS"))
                .shippingTime(new ShippingTime("24", "48"))
                .build();

        // then
        assertEquals(Money.of("12.99", "PLN"), rate.firstItemRate());
        assertEquals(Money.of("2.00", "PLN"), rate.nextItemRate());
        assertEquals(10, rate.maxQuantityPerPackage());
        assertEquals("KILOGRAMS", rate.maxPackageWeight().unit());
        assertEquals("48", rate.shippingTime().toTime());
    }

    @Test
    void rateBuilder_toBuilder_preservesFields() {
        // given
        ShippingRate original = minimalRate()
                .maxPackageWeight(new Weight("30.0", "KILOGRAMS")).build();

        // when
        ShippingRate copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void rateBuilder_whenDeliveryMethodIdMissing_throws() {
        var builder = ShippingRate.builder()
                .firstItemRate(Money.of("12.99", "PLN"))
                .nextItemRate(Money.of("2.00", "PLN")).maxQuantityPerPackage(10);
        assertMessage("ShippingRate.deliveryMethodId is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void rateBuilder_whenFirstItemRateMissing_throws() {
        var builder = ShippingRate.builder().deliveryMethodId(METHOD_ID)
                .nextItemRate(Money.of("2.00", "PLN")).maxQuantityPerPackage(10);
        assertMessage("ShippingRate.firstItemRate is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void rateBuilder_whenNextItemRateMissing_throws() {
        var builder = ShippingRate.builder().deliveryMethodId(METHOD_ID)
                .firstItemRate(Money.of("12.99", "PLN")).maxQuantityPerPackage(10);
        assertMessage("ShippingRate.nextItemRate is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void rateBuilder_whenMaxQuantityMissing_throws() {
        var builder = ShippingRate.builder().deliveryMethodId(METHOD_ID)
                .firstItemRate(Money.of("12.99", "PLN")).nextItemRate(Money.of("2.00", "PLN"));
        assertMessage("ShippingRate.maxQuantityPerPackage is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    // ---- ShippingRateSetRequestBuilder ----

    private static final String SET_NAME = "Domestic rates";

    @Test
    void rateSetBuilder_requiredFieldsOnly_buildsWithNullOptionals() {
        // when
        ShippingRateSetRequest request = ShippingRateSetRequest.builder()
                .name(SET_NAME).rates(List.of(minimalRate().build())).build();

        // then
        assertEquals(SET_NAME, request.name());
        assertEquals(1, request.rates().size());
        assertNull(request.type());
        assertNull(request.dispatchCountry());
    }

    @Test
    void rateSetBuilder_allFieldsSet_builds() {
        // when
        ShippingRateSetRequest request = ShippingRateSetRequest.builder()
                .name(SET_NAME).type(RateSetType.PHYSICAL).dispatchCountry(COUNTRY_CODE)
                .rates(List.of(minimalRate().build())).build();

        // then
        assertEquals(RateSetType.PHYSICAL, request.type());
        assertEquals(COUNTRY_CODE, request.dispatchCountry());
    }

    @Test
    void rateSetBuilder_toBuilder_preservesFields() {
        // given
        ShippingRateSetRequest original = ShippingRateSetRequest.builder()
                .name(SET_NAME).type(RateSetType.PHYSICAL)
                .rates(List.of(minimalRate().build())).build();

        // when
        ShippingRateSetRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void rateSetBuilder_whenNameMissing_throws() {
        var builder = ShippingRateSetRequest.builder().rates(List.of(minimalRate().build()));
        assertMessage("ShippingRateSetRequest.name is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    @Test
    void rateSetBuilder_whenRatesEmpty_throws() {
        var builder = ShippingRateSetRequest.builder().name(SET_NAME);
        assertMessage("ShippingRateSetRequest.rates is required",
                assertThrows(IllegalStateException.class, builder::build));
    }

    private static void assertMessage(String expected, IllegalStateException actual) {
        assertEquals(expected, actual.getMessage());
    }
}
