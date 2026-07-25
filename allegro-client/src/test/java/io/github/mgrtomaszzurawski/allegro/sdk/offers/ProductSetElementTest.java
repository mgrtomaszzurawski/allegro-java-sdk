/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementQuantityQuantityRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsibleProducerIdRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsibleProducerNameRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsiblePersonRequestResponsiblePersonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementResponsibleProducerRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestV1AllOfProductSetRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetAllOfResponsiblePersonRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetAllOfResponsibleProducerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductIdType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ResponsiblePersonRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ResponsibleProducerRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SafetyInformation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductSetElementTest {

    private static final String PRODUCT_ID = "8f2b1c00-0000-4000-8000-000000000001";
    private static final String PRODUCER_ID = "44444444-4444-4444-4444-444444444444";
    private static final String PRODUCER_NAME = "ACME Manufacturing";
    private static final String PERSON_ID = "817ab828-255e-4ca8-a4da-c6defa3e6918";
    private static final String PERSON_NAME = "Responsible EU Operator";
    private static final int QUANTITY = 3;
    private static final String TYPE_ID = "ID";
    private static final String TYPE_NAME = "NAME";
    private static final String PARAM_ID = "223545";
    private static final String PARAM_NAME = "Tytuł";
    private static final String PARAM_VALUE = "Nauka duża książka dla małych dzieci";

    @Test
    void of_whenOnlyProductId_defaultsToOneUnitNoGpsr() {
        // when
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID);

        // then
        assertEquals(PRODUCT_ID, element.productId());
        assertEquals(1, element.quantity());
        assertNull(element.responsibleProducer());
        assertNull(element.marketedBeforeGpsrObligation());
        assertTrue(element.productParameters().isEmpty());
        assertNull(element.aiCoCreated());
        assertNull(element.idType());
        assertNull(element.responsiblePerson());
    }

    @Test
    void withIdType_whenGtin_referencesProductByGtinInTheBody() {
        // given — a product referenced by its GTIN barcode rather than a catalogue id
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID).withIdType(ProductIdType.GTIN);

        // then — the flag is exposed and mapped onto the product's idType
        assertEquals(ProductIdType.GTIN, element.idType());
        assertEquals(ProductOfferRaw.IdTypeEnum.GTIN, element.toRaw().getProduct().getIdType());
    }

    @Test
    void withIdType_whenMpn_mapsToTheGeneratedIdType() {
        // then
        assertEquals(ProductOfferRaw.IdTypeEnum.MPN,
                ProductSetElement.of(PRODUCT_ID).withIdType(ProductIdType.MPN).toRaw().getProduct().getIdType());
    }

    @Test
    void toRaw_whenNoIdType_leavesProductIdTypeUnset() {
        // then — a plain catalogue-id reference does not write an idType
        assertNull(ProductSetElement.of(PRODUCT_ID).toRaw().getProduct().getIdType());
    }

    @Test
    void withCopies_preservePreviouslySetIdType() {
        // given — an idType set first, then another wither applied
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withIdType(ProductIdType.GTIN)
                .withResponsibleProducer(ResponsibleProducerRef.byId(PRODUCER_ID));

        // then — the later copy carries the earlier idType forward
        assertEquals(ProductIdType.GTIN, element.idType());
    }

    @Test
    void of_whenQuantityBelowOne_throws() {
        // then
        assertThrows(IllegalArgumentException.class, () -> ProductSetElement.of(PRODUCT_ID, 0));
    }

    @Test
    void of_whenProductIdNull_throws() {
        // then
        assertThrows(NullPointerException.class, () -> ProductSetElement.of(null));
    }

    @Test
    void withCopies_setOptionalGpsrFieldsWithoutMutating() {
        // given
        ProductSetElement base = ProductSetElement.of(PRODUCT_ID, QUANTITY);

        // when
        ProductSetElement enriched = base
                .withResponsibleProducer(ResponsibleProducerRef.byId(PRODUCER_ID))
                .withMarketedBeforeGpsrObligation(true);

        // then — the copy carries the fields, the original is untouched
        assertEquals(PRODUCER_ID, requireProducer(enriched).id());
        assertEquals(Boolean.TRUE, enriched.marketedBeforeGpsrObligation());
        assertNull(base.responsibleProducer());
        assertNull(base.marketedBeforeGpsrObligation());
    }

    @Test
    void toRaw_whenProducerById_writesProductQuantityAndIdVariant() {
        // given
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID, QUANTITY)
                .withResponsibleProducer(ResponsibleProducerRef.byId(PRODUCER_ID))
                .withMarketedBeforeGpsrObligation(false);

        // when
        SaleProductOfferRequestV1AllOfProductSetRaw raw = element.toRaw();

        // then
        assertEquals(PRODUCT_ID, raw.getProduct().getId());
        ProductSetElementQuantityQuantityRaw quantity = raw.getQuantity();
        assertEquals(QUANTITY, quantity.getValue());
        ProductSetElementResponsibleProducerRequestRaw producer = raw.getResponsibleProducer();
        ProductSetElementResponsibleProducerIdRequestRaw byId =
                assertInstanceOf(ProductSetElementResponsibleProducerIdRequestRaw.class, producer);
        assertEquals(TYPE_ID, byId.getType());
        assertEquals(PRODUCER_ID, byId.getId());
        assertEquals(Boolean.FALSE, raw.getMarketedBeforeGPSRObligation());
    }

    @Test
    void toRaw_whenProducerByName_writesNameVariant() {
        // given
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withResponsibleProducer(ResponsibleProducerRef.byName(PRODUCER_NAME));

        // when
        ProductSetElementResponsibleProducerRequestRaw producer = element.toRaw().getResponsibleProducer();

        // then
        ProductSetElementResponsibleProducerNameRequestRaw byName =
                assertInstanceOf(ProductSetElementResponsibleProducerNameRequestRaw.class, producer);
        assertEquals(TYPE_NAME, byName.getType());
        assertEquals(PRODUCER_NAME, byName.getName());
    }

    @Test
    void toRaw_whenNoGpsr_omitsProducerAndMarketed() {
        // when
        SaleProductOfferRequestV1AllOfProductSetRaw raw = ProductSetElement.of(PRODUCT_ID).toRaw();

        // then
        assertNull(raw.getResponsibleProducer());
        assertNull(raw.getMarketedBeforeGPSRObligation());
    }

    @Test
    void producerRef_whenBothIdAndName_throws() {
        // then — the canonical constructor enforces exactly one form
        assertThrows(IllegalArgumentException.class,
                () -> new ResponsibleProducerRef(PRODUCER_ID, PRODUCER_NAME));
    }

    @Test
    void producerRef_whenNeitherIdNorName_throws() {
        // then
        assertThrows(IllegalArgumentException.class,
                () -> new ResponsibleProducerRef(null, null));
    }

    @Test
    void from_mapsProductQuantityProducerAndMarketed() {
        // given — a productized response element (producer id-only, as Allegro returns it)
        SaleProductOfferResponseV1AllOfProductSetRaw raw =
                new SaleProductOfferResponseV1AllOfProductSetRaw()
                        .product(new SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw().id(PRODUCT_ID)
                                .parameters(List.of(new ParameterProductOfferResponseRaw()
                                        .id(PARAM_ID).name(PARAM_NAME).values(List.of(PARAM_VALUE))))
                                .isAiCoCreated(true))
                        .quantity(new ProductSetElementQuantityQuantityRaw().value(QUANTITY))
                        .responsibleProducer(new SaleProductOfferResponseV1AllOfProductSetAllOfResponsibleProducerRaw()
                                .id(PRODUCER_ID))
                        .marketedBeforeGPSRObligation(true);

        // when
        ProductSetElement element = ProductSetElement.from(raw);

        // then
        assertEquals(PRODUCT_ID, element.productId());
        assertEquals(QUANTITY, element.quantity());
        assertEquals(PRODUCER_ID, requireProducer(element).id());
        assertNull(requireProducer(element).name());
        assertEquals(Boolean.TRUE, element.marketedBeforeGpsrObligation());
        // the bound product's catalogue parameters and AI-co-created flag read back
        assertEquals(1, element.productParameters().size());
        assertEquals(PARAM_ID, element.productParameters().get(0).id());
        assertEquals(PARAM_NAME, element.productParameters().get(0).name());
        assertEquals(List.of(PARAM_VALUE), element.productParameters().get(0).values());
        assertEquals(Boolean.TRUE, element.aiCoCreated());
    }

    @Test
    void from_whenQuantityMissing_defaultsToOne() {
        // given
        SaleProductOfferResponseV1AllOfProductSetRaw raw =
                new SaleProductOfferResponseV1AllOfProductSetRaw()
                        .product(new SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw().id(PRODUCT_ID));

        // when
        ProductSetElement element = ProductSetElement.from(raw);

        // then
        assertEquals(1, element.quantity());
        assertNull(element.responsibleProducer());
        assertTrue(element.marketedBeforeGpsrObligation() == null);
        // the product carried no parameters block: the null-parameters branch degrades to empty
        assertTrue(element.productParameters().isEmpty());
    }

    @Test
    void withResponsiblePerson_whenById_writesFlatIdForm() {
        // given — a person referenced by its registered id
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withResponsiblePerson(ResponsiblePersonRef.byId(PERSON_ID));

        // then — the flat {id} block is written, name omitted
        assertEquals(PERSON_ID, requirePerson(element).id());
        ProductSetElementResponsiblePersonRequestResponsiblePersonRaw person =
                element.toRaw().getResponsiblePerson();
        assertEquals(PERSON_ID, person.getId());
        assertNull(person.getName());
    }

    @Test
    void withResponsiblePerson_whenByName_writesFlatNameForm() {
        // given — a person referenced by name (Allegro resolves it to a stored operator)
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withResponsiblePerson(ResponsiblePersonRef.byName(PERSON_NAME));

        // then — the flat {name} block is written, id omitted
        ProductSetElementResponsiblePersonRequestResponsiblePersonRaw person =
                element.toRaw().getResponsiblePerson();
        assertEquals(PERSON_NAME, person.getName());
        assertNull(person.getId());
    }

    @Test
    void withResponsiblePerson_preservedByLaterCopies() {
        // given — a person set first, then another wither applied
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withResponsiblePerson(ResponsiblePersonRef.byId(PERSON_ID))
                .withMarketedBeforeGpsrObligation(true);

        // then — the later copy carries the earlier person forward
        assertEquals(PERSON_ID, requirePerson(element).id());
    }

    @Test
    void toRaw_whenNoResponsiblePerson_omitsIt() {
        // then — a plain element writes no responsiblePerson block
        assertNull(ProductSetElement.of(PRODUCT_ID).toRaw().getResponsiblePerson());
    }

    @Test
    void from_whenResponsiblePersonPresent_mapsIdOnly() {
        // given — Allegro returns the person id-only on read
        SaleProductOfferResponseV1AllOfProductSetRaw raw =
                new SaleProductOfferResponseV1AllOfProductSetRaw()
                        .product(new SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw().id(PRODUCT_ID))
                        .responsiblePerson(new SaleProductOfferResponseV1AllOfProductSetAllOfResponsiblePersonRaw()
                                .id(PERSON_ID));

        // when
        ProductSetElement element = ProductSetElement.from(raw);

        // then
        assertEquals(PERSON_ID, requirePerson(element).id());
        assertNull(requirePerson(element).name());
    }

    @Test
    void from_whenResponsiblePersonAbsent_leavesItNull() {
        // given — a response element with no responsiblePerson block
        SaleProductOfferResponseV1AllOfProductSetRaw raw =
                new SaleProductOfferResponseV1AllOfProductSetRaw()
                        .product(new SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw().id(PRODUCT_ID));

        // then
        assertNull(ProductSetElement.from(raw).responsiblePerson());
    }

    @Test
    void personRef_whenBothIdAndName_throws() {
        // then — the canonical constructor enforces exactly one form
        assertThrows(IllegalArgumentException.class,
                () -> new ResponsiblePersonRef(PERSON_ID, PERSON_NAME));
    }

    @Test
    void personRef_whenNeitherIdNorName_throws() {
        // then
        assertThrows(IllegalArgumentException.class,
                () -> new ResponsiblePersonRef(null, null));
    }

    @Test
    void withSafetyInformation_whenText_writesTheSafetyBlock() {
        // given — an explicit TEXT safety declaration attached to the element
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withSafetyInformation(SafetyInformation.text("Keep dry."));

        // then — the element exposes it and toRaw emits the request safety block
        assertEquals(SafetyInformation.TEXT, element.safetyInformation().type());
        assertNotNull(element.toRaw().getSafetyInformation());
    }

    @Test
    void withSafetyInformation_preservedByLaterCopies() {
        // given — safety set first, then another wither applied
        ProductSetElement element = ProductSetElement.of(PRODUCT_ID)
                .withSafetyInformation(SafetyInformation.text("Keep dry."))
                .withResponsiblePerson(ResponsiblePersonRef.byId(PERSON_ID));

        // then — the later copy carries the earlier safety information forward
        assertEquals(SafetyInformation.TEXT, element.safetyInformation().type());
    }

    @Test
    void toRaw_whenNoSafetyInformation_omitsIt() {
        // then — a plain element writes no safetyInformation block
        assertNull(ProductSetElement.of(PRODUCT_ID).toRaw().getSafetyInformation());
    }

    private static ResponsiblePersonRef requirePerson(ProductSetElement element) {
        ResponsiblePersonRef person = element.responsiblePerson();
        if (person == null) {
            throw new AssertionError("expected a responsible person");
        }
        return person;
    }

    private static ResponsibleProducerRef requireProducer(ProductSetElement element) {
        ResponsibleProducerRef producer = element.responsibleProducer();
        if (producer == null) {
            throw new AssertionError("expected a responsible producer");
        }
        return producer;
    }
}
