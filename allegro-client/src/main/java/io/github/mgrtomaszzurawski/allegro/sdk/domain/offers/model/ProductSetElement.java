/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementQuantityQuantityRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestV1AllOfProductSetRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1AllOfProductSetRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One element of an offer's {@code productSet} — the binding of the offer to a catalogue
 * product. A single-product offer has one element referencing the product by id; a set or
 * multipack has several, each with its own {@code quantity}.
 *
 * <p>This value references an EXISTING catalogue product by {@link #productId() id} (a
 * product UUID). Defining a brand-new product inline, product attachments, and
 * {@code deposits} are not modelled here yet. On the WRITE side this element covers the
 * product reference plus the GPSR {@linkplain ResponsibleProducerRef responsible producer},
 * the {@linkplain ResponsiblePersonRef responsible person}, the pre-obligation marker and the
 * GPSR {@link #safetyInformation() safety information}, which is what a productized category
 * requires to be created; on the READ side it additionally surfaces the product's catalogue
 * {@link #productParameters() parameters} and its {@link #aiCoCreated() AI-co-created flag}.
 *
 * <p>The same immutable value is used both ways: build one for {@code CreateOfferRequest}, or
 * read one back from an {@link Offer}. Optional fields are added with the {@code with…}
 * copies.
 *
 * @param productId                    the catalogue product id (required)
 * @param quantity                     units of the product in this element (at least 1)
 * @param responsibleProducer          the GPSR responsible producer, or {@code null}
 * @param marketedBeforeGpsrObligation {@code true}/{@code false} to declare the product was
 *                                     placed on the market before the GPSR obligation, or
 *                                     {@code null} to leave it unset
 * @param productParameters            the bound product's catalogue parameters as read back
 *                                     (empty on a build-by-id element or when the payload omits them)
 * @param aiCoCreated                  {@code true} if the bound product's content was AI co-created,
 *                                     as reported by Allegro, or {@code null}
 * @param safetyInformation            the product's GPSR safety information (text/attachments/none)
 *                                     — read back from a response, or set for a write via
 *                                     {@link #withSafetyInformation(SafetyInformation)}; {@code null}
 *                                     when the payload omits it
 * @param idType                       how {@code productId} identifies the product on a WRITE —
 *                                     {@code null} means an Allegro catalogue product id, or set
 *                                     {@link ProductIdType#GTIN}/{@link ProductIdType#MPN} to
 *                                     reference it by a manufacturer identifier
 * @param responsiblePerson            the GPSR responsible person (EU compliance operator), or
 *                                     {@code null}; read back id-only
 * @since 0.4.0
 */
public record ProductSetElement(
        String productId,
        int quantity,
        @Nullable ResponsibleProducerRef responsibleProducer,
        @Nullable Boolean marketedBeforeGpsrObligation,
        List<OfferParameter> productParameters,
        @Nullable Boolean aiCoCreated,
        @Nullable SafetyInformation safetyInformation,
        @Nullable ProductIdType idType,
        @Nullable ResponsiblePersonRef responsiblePerson) {

    private static final String ERR_QUANTITY = "quantity must be at least 1";
    private static final int DEFAULT_QUANTITY = 1;

    /**
     * Canonical constructor: the product id is required and the quantity must be positive;
     * {@code productParameters} is normalized to an immutable copy (empty when the payload,
     * or a build path that only references the product by id, omits them).
     */
    public ProductSetElement {
        Objects.requireNonNull(productId, "productId");
        if (quantity < DEFAULT_QUANTITY) {
            throw new IllegalArgumentException(ERR_QUANTITY);
        }
        productParameters = productParameters == null ? List.of() : List.copyOf(productParameters);
    }

    /** A single unit of the given catalogue product. */
    public static ProductSetElement of(String productId) {
        return new ProductSetElement(productId, DEFAULT_QUANTITY, null, null, List.of(), null, null, null, null);
    }

    /** {@code quantity} units of the given catalogue product. */
    public static ProductSetElement of(String productId, int quantity) {
        return new ProductSetElement(productId, quantity, null, null, List.of(), null, null, null, null);
    }

    /** A copy of this element with the GPSR responsible producer set. */
    public ProductSetElement withResponsibleProducer(ResponsibleProducerRef producer) {
        return new ProductSetElement(productId, quantity,
                Objects.requireNonNull(producer, "producer"), marketedBeforeGpsrObligation,
                productParameters, aiCoCreated, safetyInformation, idType, responsiblePerson);
    }

    /** A copy of this element with the GPSR pre-obligation marker set. */
    public ProductSetElement withMarketedBeforeGpsrObligation(boolean marketed) {
        return new ProductSetElement(productId, quantity, responsibleProducer, marketed,
                productParameters, aiCoCreated, safetyInformation, idType, responsiblePerson);
    }

    /**
     * A copy of this element that references the product by a manufacturer identifier: the
     * {@code productId} is read as a {@link ProductIdType#GTIN GTIN} or
     * {@link ProductIdType#MPN MPN} rather than an Allegro catalogue id.
     */
    public ProductSetElement withIdType(ProductIdType idType) {
        return new ProductSetElement(productId, quantity, responsibleProducer,
                marketedBeforeGpsrObligation, productParameters, aiCoCreated, safetyInformation,
                Objects.requireNonNull(idType, "idType"), responsiblePerson);
    }

    /** A copy of this element with the GPSR responsible person set. */
    public ProductSetElement withResponsiblePerson(ResponsiblePersonRef person) {
        return new ProductSetElement(productId, quantity, responsibleProducer,
                marketedBeforeGpsrObligation, productParameters, aiCoCreated, safetyInformation,
                idType, Objects.requireNonNull(person, "person"));
    }

    /**
     * A copy of this element with the GPSR safety information to send set. Build the argument
     * with {@link SafetyInformation#text(String)} or {@link SafetyInformation#attachments(List)}
     * — the "none" form cannot be sent on a request.
     */
    public ProductSetElement withSafetyInformation(SafetyInformation safetyInformation) {
        return new ProductSetElement(productId, quantity, responsibleProducer,
                marketedBeforeGpsrObligation, productParameters, aiCoCreated,
                Objects.requireNonNull(safetyInformation, "safetyInformation"), idType, responsiblePerson);
    }

    /** Project a generated response product-set element onto the consumer value. */
    public static ProductSetElement from(SaleProductOfferResponseV1AllOfProductSetRaw raw) {
        var product = raw.getProduct();
        var quantityRaw = raw.getQuantity();
        int units = quantityRaw == null || quantityRaw.getValue() == null
                ? DEFAULT_QUANTITY : quantityRaw.getValue();
        var producer = raw.getResponsibleProducer();
        var person = raw.getResponsiblePerson();
        return new ProductSetElement(
                Objects.requireNonNull(product == null ? null : product.getId(), "product.id"),
                units,
                producer == null ? null : ResponsibleProducerRef.from(producer),
                raw.getMarketedBeforeGPSRObligation(),
                productParametersOf(product),
                product == null ? null : product.getIsAiCoCreated(),
                SafetyInformation.from(raw.getSafetyInformation()),
                null,
                person == null ? null : ResponsiblePersonRef.from(person));
    }

    private static List<OfferParameter> productParametersOf(
            @Nullable SaleProductOfferResponseV1AllOfProductSetAllOfProductRaw product) {
        List<ParameterProductOfferResponseRaw> parameters = product == null ? null : product.getParameters();
        return parameters == null ? List.of() : parameters.stream().map(OfferParameter::from).toList();
    }

    /** The generated request element: the product reference, the quantity, and any GPSR fields. */
    public SaleProductOfferRequestV1AllOfProductSetRaw toRaw() {
        ProductOfferRaw productRaw = new ProductOfferRaw().id(productId);
        if (idType != null) {
            productRaw.idType(idType.toRaw());
        }
        SaleProductOfferRequestV1AllOfProductSetRaw raw =
                new SaleProductOfferRequestV1AllOfProductSetRaw()
                        .product(productRaw)
                        .quantity(new ProductSetElementQuantityQuantityRaw().value(quantity));
        if (responsibleProducer != null) {
            raw.responsibleProducer(responsibleProducer.toRaw());
        }
        if (marketedBeforeGpsrObligation != null) {
            raw.marketedBeforeGPSRObligation(marketedBeforeGpsrObligation);
        }
        if (responsiblePerson != null) {
            raw.responsiblePerson(responsiblePerson.toRaw());
        }
        if (safetyInformation != null) {
            raw.safetyInformation(safetyInformation.toRaw());
        }
        return raw;
    }
}
