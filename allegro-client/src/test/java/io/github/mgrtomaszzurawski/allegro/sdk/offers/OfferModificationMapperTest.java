/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.HandlingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferDuration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PaymentsModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.OfferModificationMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Wire-shape mapping of {@link OfferModificationMapper}: each domain duration /
 * handling-time value maps to its ISO 8601 wire token, the two publication modes
 * (fixed vs unlimited), and the omission of the eight unset {@code Modification}
 * sub-objects. Assertions are on the serialized JSON tree; NON_EMPTY mirrors the
 * SDK's partial write body.
 */
class OfferModificationMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private static final String OFFER_ONE = "111";
    private static final String OFFER_TWO = "222";
    private static final String DURATION_PATH = "/modification/publication/duration";
    private static final String UNLIMITED_PATH = "/modification/publication/durationUnlimited";
    private static final String HANDLING_TIME_PATH = "/modification/delivery/handlingTime";
    private static final String REFERENCE_ID = "ref-9f3a";
    private static final String SHIPPING_RATES_PATH = "/modification/delivery/shippingRates/id";
    private static final String WHOLESALE_PATH = "/modification/discounts/wholesalePriceList/id";
    private static final String SIZE_TABLE_PATH = "/modification/sizeTable/id";
    private static final String SERVICES_GROUP_PATH = "/modification/additionalServicesGroup/id";
    private static final String RESPONSIBLE_PRODUCER_PATH = "/modification/responsibleProducer/id";
    private static final String RESPONSIBLE_PERSON_PATH = "/modification/responsiblePerson/id";
    private static final String PAYMENTS_INVOICE_PATH = "/modification/payments/invoice";
    private static final String PAYMENTS_TAX_PERCENTAGE_PATH = "/modification/payments/tax/percentage";
    private static final String VAT_RATE = "23";

    private static JsonNode tree(BatchModificationRequest request) {
        return MAPPER.valueToTree(OfferModificationMapper.toRaw(request));
    }

    private static BatchModificationRequest.Builder forOne() {
        return BatchModificationRequest.forOffers(List.of(OFFER_ONE));
    }

    static List<Arguments> durationWireTokens() {
        return List.of(
                Arguments.of(OfferDuration.DAYS_3, "P3D"),
                Arguments.of(OfferDuration.DAYS_5, "P5D"),
                Arguments.of(OfferDuration.DAYS_7, "P7D"),
                Arguments.of(OfferDuration.DAYS_10, "P10D"),
                Arguments.of(OfferDuration.DAYS_20, "P20D"),
                Arguments.of(OfferDuration.DAYS_30, "P30D"));
    }

    @ParameterizedTest
    @MethodSource("durationWireTokens")
    void toRaw_whenListingDuration_mapsToIsoDurationToken(OfferDuration duration, String wireToken) {
        // given — a fixed listing duration
        BatchModificationRequest request = forOne().listingDuration(duration).build();

        // then — it serializes as its ISO 8601 day token under publication.duration
        assertEquals(wireToken, tree(request).at(DURATION_PATH).asText());
    }

    static List<Arguments> handlingTimeWireTokens() {
        return List.of(
                Arguments.of(HandlingTime.IMMEDIATE, "PT0S"),
                Arguments.of(HandlingTime.DAY_1, "PT24H"),
                Arguments.of(HandlingTime.DAYS_2, "P2D"),
                Arguments.of(HandlingTime.DAYS_3, "P3D"),
                Arguments.of(HandlingTime.DAYS_4, "P4D"),
                Arguments.of(HandlingTime.DAYS_5, "P5D"),
                Arguments.of(HandlingTime.DAYS_7, "P7D"),
                Arguments.of(HandlingTime.DAYS_10, "P10D"),
                Arguments.of(HandlingTime.DAYS_14, "P14D"),
                Arguments.of(HandlingTime.DAYS_21, "P21D"),
                Arguments.of(HandlingTime.DAYS_30, "P30D"),
                Arguments.of(HandlingTime.DAYS_60, "P60D"));
    }

    @ParameterizedTest
    @MethodSource("handlingTimeWireTokens")
    void toRaw_whenHandlingTime_mapsToIsoDurationToken(HandlingTime handlingTime, String wireToken) {
        // given — a dispatch time
        BatchModificationRequest request = forOne().handlingTime(handlingTime).build();

        // then — it serializes as its ISO 8601 token under delivery.handlingTime
        assertEquals(wireToken, tree(request).at(HANDLING_TIME_PATH).asText());
    }

    @Test
    void toRaw_whenUnlimitedListing_setsDurationUnlimitedAndNoDuration() {
        // given — an unlimited listing
        BatchModificationRequest request = forOne().unlimitedListing().build();

        // when
        JsonNode tree = tree(request);

        // then — durationUnlimited is true and no fixed duration is sent
        assertTrue(tree.at(UNLIMITED_PATH).asBoolean());
        assertTrue(tree.at(DURATION_PATH).isMissingNode());
    }

    @Test
    void toRaw_whenOnlyDuration_omitsUnsetSubObjectsAndCarriesCriteria() {
        // given — only a listing duration on two offers
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ONE, OFFER_TWO))
                .listingDuration(OfferDuration.DAYS_7)
                .build();

        // when
        JsonNode tree = tree(request);

        // then — delivery and the other unset sub-objects are absent (partial body)...
        assertTrue(tree.at("/modification/delivery").isMissingNode());
        assertTrue(tree.at("/modification/discounts").isMissingNode());
        assertTrue(tree.at("/modification/payments").isMissingNode());
        // ...and both offers ride in one CONTAINS_OFFERS criterion
        assertEquals("CONTAINS_OFFERS", tree.at("/offerCriteria/0/type").asText());
        assertEquals(OFFER_ONE, tree.at("/offerCriteria/0/offers/0/id").asText());
        assertEquals(OFFER_TWO, tree.at("/offerCriteria/0/offers/1/id").asText());
    }

    @Test
    void toRaw_whenHandlingTimeOnly_omitsPublication() {
        // given — only a handling time change
        BatchModificationRequest request = forOne().handlingTime(HandlingTime.DAYS_2).build();

        // when
        JsonNode tree = tree(request);

        // then — delivery is present and publication is absent (single-element command)
        assertEquals("P2D", tree.at(HANDLING_TIME_PATH).asText());
        assertTrue(tree.at("/modification/publication").isMissingNode());
    }

    @Test
    void toRaw_whenShippingRatesAssigned_mapsIdUnderDeliveryShippingRates() {
        BatchModificationRequest request = forOne().shippingRates(REFERENCE_ID).build();
        JsonNode tree = tree(request);
        assertEquals(REFERENCE_ID, tree.at(SHIPPING_RATES_PATH).asText());
        // the single-element rule holds: no handling time rides along under delivery
        assertTrue(tree.at(HANDLING_TIME_PATH).isMissingNode());
    }

    @Test
    void toRaw_whenWholesalePriceListAssigned_mapsIdUnderDiscounts() {
        BatchModificationRequest request = forOne().wholesalePriceList(REFERENCE_ID).build();
        assertEquals(REFERENCE_ID, tree(request).at(WHOLESALE_PATH).asText());
    }

    @Test
    void toRaw_whenSizeTableAssigned_mapsIdUnderSizeTable() {
        BatchModificationRequest request = forOne().sizeTable(REFERENCE_ID).build();
        assertEquals(REFERENCE_ID, tree(request).at(SIZE_TABLE_PATH).asText());
    }

    @Test
    void toRaw_whenAdditionalServicesGroupAssigned_mapsIdUnderAdditionalServicesGroup() {
        BatchModificationRequest request = forOne().additionalServicesGroup(REFERENCE_ID).build();
        assertEquals(REFERENCE_ID, tree(request).at(SERVICES_GROUP_PATH).asText());
    }

    @Test
    void toRaw_whenResponsibleProducerAssigned_mapsIdUnderResponsibleProducer() {
        BatchModificationRequest request = forOne().responsibleProducer(REFERENCE_ID).build();
        assertEquals(REFERENCE_ID, tree(request).at(RESPONSIBLE_PRODUCER_PATH).asText());
    }

    @Test
    void toRaw_whenResponsiblePersonAssigned_mapsIdUnderResponsiblePerson() {
        BatchModificationRequest request = forOne().responsiblePerson(REFERENCE_ID).build();
        assertEquals(REFERENCE_ID, tree(request).at(RESPONSIBLE_PERSON_PATH).asText());
    }

    @Test
    void toRaw_whenPaymentsInvoiceAndVatRate_mapsBothUnderPayments() {
        // given — both invoice type and VAT rate change together (one payments element)
        PaymentsModification payments = PaymentsModification.builder()
                .invoiceType(InvoiceType.VAT).vatRate(VAT_RATE)
                .build();
        BatchModificationRequest request = forOne().payments(payments).build();

        // when
        JsonNode tree = tree(request);

        // then — invoice enum token and tax.percentage both present under payments
        assertEquals("VAT", tree.at(PAYMENTS_INVOICE_PATH).asText());
        assertEquals(VAT_RATE, tree.at(PAYMENTS_TAX_PERCENTAGE_PATH).asText());
        assertTrue(tree.at("/modification/publication").isMissingNode());
    }

    @Test
    void toRaw_whenPaymentsInvoiceOnly_omitsTax() {
        // given — only the invoice type changes
        PaymentsModification payments = PaymentsModification.builder()
                .invoiceType(InvoiceType.VAT_MARGIN)
                .build();
        BatchModificationRequest request = forOne().payments(payments).build();

        // when
        JsonNode tree = tree(request);

        // then — invoice present, tax absent (partial payments body)
        assertEquals("VAT_MARGIN", tree.at(PAYMENTS_INVOICE_PATH).asText());
        assertTrue(tree.at("/modification/payments/tax").isMissingNode());
    }

    @Test
    void toRaw_whenPaymentsVatRateOnly_omitsInvoice() {
        // given — only the VAT rate changes
        PaymentsModification payments = PaymentsModification.builder().vatRate(VAT_RATE).build();
        BatchModificationRequest request = forOne().payments(payments).build();

        // when
        JsonNode tree = tree(request);

        // then — tax.percentage present, invoice absent
        assertEquals(VAT_RATE, tree.at(PAYMENTS_TAX_PERCENTAGE_PATH).asText());
        assertTrue(tree.at(PAYMENTS_INVOICE_PATH).isMissingNode());
    }
}
