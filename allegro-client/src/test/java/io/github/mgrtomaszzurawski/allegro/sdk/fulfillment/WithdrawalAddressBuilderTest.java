/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.WithdrawalAddressBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import org.junit.jupiter.api.Test;

class WithdrawalAddressBuilderTest {

    private static final String COMPANY = "Warehouse Sp. z o.o.";
    private static final String STREET = "Uliczna 7";
    private static final String POSTAL_CODE = "60-166";
    private static final String CITY = "Poznan";
    private static final String COUNTRY_CODE = "PL";
    private static final String ADDITIONAL_INFO = "Gate 3";
    private static final int MAX_COMPANY_LENGTH = 200;
    private static final int MAX_STREET_LENGTH = 150;
    private static final int MAX_POSTAL_CODE_LENGTH = 12;
    private static final int MAX_CITY_LENGTH = 50;
    private static final int MAX_ADDITIONAL_INFO_LENGTH = 300;

    private static final String FIELD_COMPANY = "company";
    private static final String FIELD_STREET = "street";
    private static final String FIELD_POSTAL_CODE = "postalCode";
    private static final String FIELD_CITY = "city";
    private static final String FIELD_COUNTRY_CODE = "countryCode";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_ADDITIONAL_INFO = "additionalInfo";

    private static PhoneNumber phone() {
        return PhoneNumber.of("48", "123123123");
    }

    private static WithdrawalAddressBuilder required() {
        return new WithdrawalAddressBuilder()
                .company(COMPANY)
                .street(STREET)
                .postalCode(POSTAL_CODE)
                .city(CITY)
                .countryCode(COUNTRY_CODE)
                .phone(phone());
    }

    /** Build must fail with a message that names the offending field. */
    private static void assertBuildRejects(WithdrawalAddressBuilder builder, String fieldToken) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(fieldToken),
                () -> "message should identify '" + fieldToken + "' but was: " + failure.getMessage());
    }

    @Test
    void build_whenOnlyRequiredFieldsSet_buildsAddressWithoutAdditionalInfo() {
        // when
        WithdrawalAddress address = required().build();

        // then
        assertEquals(COMPANY, address.company());
        assertEquals(COUNTRY_CODE, address.countryCode());
        assertNull(address.additionalInfo());
    }

    @Test
    void build_whenAllCoreFieldsSet_buildsFullAddress() {
        // when
        WithdrawalAddress address = required().additionalInfo(ADDITIONAL_INFO).build();

        // then
        assertEquals(STREET, address.street());
        assertEquals(POSTAL_CODE, address.postalCode());
        assertEquals(CITY, address.city());
        assertEquals("123123123", address.phone().number());
        assertEquals(ADDITIONAL_INFO, address.additionalInfo());
    }

    @Test
    void toBuilder_whenAllFieldsSet_preservesThem() {
        // given
        WithdrawalAddress original = required().additionalInfo(ADDITIONAL_INFO).build();

        // when
        WithdrawalAddress copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void toBuilder_whenNoAdditionalInfo_preservesRequiredFields() {
        // given
        WithdrawalAddress original = required().build();

        // when
        WithdrawalAddress copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
        assertNull(copy.additionalInfo());
    }

    @Test
    void build_whenCompanyMissing_throwsIdentifyingCompany() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .street(STREET).postalCode(POSTAL_CODE).city(CITY)
                .countryCode(COUNTRY_CODE).phone(phone());
        assertBuildRejects(builder, FIELD_COMPANY);
    }

    @Test
    void build_whenStreetMissing_throwsIdentifyingStreet() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .company(COMPANY).postalCode(POSTAL_CODE).city(CITY)
                .countryCode(COUNTRY_CODE).phone(phone());
        assertBuildRejects(builder, FIELD_STREET);
    }

    @Test
    void build_whenPostalCodeMissing_throwsIdentifyingPostalCode() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .company(COMPANY).street(STREET).city(CITY)
                .countryCode(COUNTRY_CODE).phone(phone());
        assertBuildRejects(builder, FIELD_POSTAL_CODE);
    }

    @Test
    void build_whenCityMissing_throwsIdentifyingCity() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .company(COMPANY).street(STREET).postalCode(POSTAL_CODE)
                .countryCode(COUNTRY_CODE).phone(phone());
        assertBuildRejects(builder, FIELD_CITY);
    }

    @Test
    void build_whenCountryCodeMissing_throwsIdentifyingCountryCode() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .company(COMPANY).street(STREET).postalCode(POSTAL_CODE).city(CITY)
                .phone(phone());
        assertBuildRejects(builder, FIELD_COUNTRY_CODE);
    }

    @Test
    void build_whenPhoneMissing_throwsIdentifyingPhone() {
        WithdrawalAddressBuilder builder = new WithdrawalAddressBuilder()
                .company(COMPANY).street(STREET).postalCode(POSTAL_CODE).city(CITY)
                .countryCode(COUNTRY_CODE);
        assertBuildRejects(builder, FIELD_PHONE);
    }

    @Test
    void build_whenCompanyAtMaxLength_succeeds() {
        WithdrawalAddress address = required().company("a".repeat(MAX_COMPANY_LENGTH)).build();
        assertEquals(MAX_COMPANY_LENGTH, address.company().length());
    }

    @Test
    void build_whenCompanyExceedsMaxLength_throwsIdentifyingCompany() {
        assertBuildRejects(required().company("a".repeat(MAX_COMPANY_LENGTH + 1)), FIELD_COMPANY);
    }

    @Test
    void build_whenStreetExceedsMaxLength_throwsIdentifyingStreet() {
        assertBuildRejects(required().street("a".repeat(MAX_STREET_LENGTH + 1)), FIELD_STREET);
    }

    @Test
    void build_whenPostalCodeExceedsMaxLength_throwsIdentifyingPostalCode() {
        assertBuildRejects(required().postalCode("a".repeat(MAX_POSTAL_CODE_LENGTH + 1)), FIELD_POSTAL_CODE);
    }

    @Test
    void build_whenCityExceedsMaxLength_throwsIdentifyingCity() {
        assertBuildRejects(required().city("a".repeat(MAX_CITY_LENGTH + 1)), FIELD_CITY);
    }

    @Test
    void build_whenAdditionalInfoAtMaxLength_succeeds() {
        WithdrawalAddress address =
                required().additionalInfo("a".repeat(MAX_ADDITIONAL_INFO_LENGTH)).build();
        assertEquals(MAX_ADDITIONAL_INFO_LENGTH, address.additionalInfo().length());
    }

    @Test
    void build_whenAdditionalInfoExceedsMaxLength_throwsIdentifyingAdditionalInfo() {
        assertBuildRejects(required().additionalInfo("a".repeat(MAX_ADDITIONAL_INFO_LENGTH + 1)),
                FIELD_ADDITIONAL_INFO);
    }

    @Test
    void toString_doesNotLeakPersonalData() {
        assertFalse(required().build().toString().contains(COMPANY));
    }
}
