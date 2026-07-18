/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ClaimFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.InvoiceDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RefundClaimRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RejectionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ReturnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.ReturnRejectionCode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Round-trip and validation tests for the bucket B order sub-facade builders. */
class OrderSubfacadeBuildersTest {

    private static final String INVOICE_NUMBER = "FV/2026/01";
    private static final String FILE_NAME = "invoice.pdf";
    private static final String REASON = "Repaired under warranty";
    private static final String LINE_ITEM_ID = "0f3e2b1a-1111-2222-3333-444455556666";
    private static final String OFFER_ID = "12345";
    private static final String ORDER_ID = "a8f6c3e2-1111-2222-3333-444455556666";
    private static final String BUYER_ID = "44556677";
    private static final String BUYER_LOGIN = "test-buyer";
    private static final String BUYER_EMAIL = "buyer@example.com";
    private static final String REFERENCE_NUMBER = "R-1";
    private static final String STATUS = "NEW";
    private static final int QUANTITY = 2;
    private static final OffsetDateTime FROM =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO =
            OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void invoiceDeclaration_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        InvoiceDeclaration declaration = InvoiceDeclaration.builder()
                .invoiceNumber(INVOICE_NUMBER).fileName(FILE_NAME).build();

        // then
        assertEquals(INVOICE_NUMBER, declaration.invoiceNumber());
        assertEquals(FILE_NAME, declaration.fileName());
        InvoiceDeclaration copy = declaration.toBuilder().build();
        assertEquals(INVOICE_NUMBER, copy.invoiceNumber());
        assertEquals(FILE_NAME, copy.fileName());
    }

    @Test
    void invoiceDeclaration_whenFileNameOnly_buildsWithNullInvoiceNumber() {
        // when — the invoice number is optional (spec marks it so)
        InvoiceDeclaration declaration = InvoiceDeclaration.builder().fileName(FILE_NAME).build();

        // then
        assertEquals(FILE_NAME, declaration.fileName());
        assertNull(declaration.invoiceNumber());
    }

    @Test
    void invoiceDeclaration_whenFileNameMissing_throwsIllegalState() {
        // then — the file name is the required field
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> InvoiceDeclaration.builder().invoiceNumber(INVOICE_NUMBER).build());
        assertTrue(failure.getMessage().contains("fileName"), failure.getMessage());
    }

    @Test
    void rejectionRequest_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        RejectionRequest request = RejectionRequest.builder()
                .code(ReturnRejectionCode.ITEM_FIXED).reason(REASON).build();

        // then
        assertEquals(ReturnRejectionCode.ITEM_FIXED, request.code());
        assertEquals(REASON, request.reason());
        RejectionRequest copy = request.toBuilder().build();
        assertEquals(ReturnRejectionCode.ITEM_FIXED, copy.code());
        assertEquals(REASON, copy.reason());
    }

    @Test
    void rejectionRequest_whenCodeOnly_buildsWithNullReason() {
        // when — reason is optional
        RejectionRequest request = RejectionRequest.builder()
                .code(ReturnRejectionCode.NO_RETURN_RIGHT).build();

        // then
        assertEquals(ReturnRejectionCode.NO_RETURN_RIGHT, request.code());
        assertNull(request.reason());
    }

    @Test
    void rejectionRequest_whenCodeMissing_throwsIllegalState() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RejectionRequest.builder().reason(REASON).build());
        assertTrue(failure.getMessage().contains("code"), failure.getMessage());
    }

    @Test
    void returnRejectionCode_whenMapped_producesMatchingRawValueForEveryConstant() {
        // then — exhaustive switch: every domain code maps to the Layer-1 enum
        for (ReturnRejectionCode code : ReturnRejectionCode.values()) {
            assertEquals(code.name(), code.toRaw().getValue());
        }
    }

    @Test
    void refundClaimRequest_whenValid_buildsAndToBuilderPreserves() {
        // when
        RefundClaimRequest request = RefundClaimRequest.builder()
                .lineItemId(LINE_ITEM_ID).quantity(QUANTITY).build();

        // then
        assertEquals(LINE_ITEM_ID, request.lineItemId());
        assertEquals(QUANTITY, request.quantity());
        RefundClaimRequest copy = request.toBuilder().build();
        assertEquals(LINE_ITEM_ID, copy.lineItemId());
        assertEquals(QUANTITY, copy.quantity());
    }

    @Test
    void refundClaimRequest_whenLineItemMissing_throwsIllegalState() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundClaimRequest.builder().quantity(QUANTITY).build());
        assertTrue(failure.getMessage().contains("lineItemId"), failure.getMessage());
    }

    @Test
    void refundClaimRequest_whenQuantityNotPositive_throwsIllegalState() {
        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RefundClaimRequest.builder().lineItemId(LINE_ITEM_ID).quantity(0).build());
        assertTrue(failure.getMessage().contains("quantity"), failure.getMessage());
    }

    @Test
    void returnFilter_whenAll_isEmpty() {
        // then
        ReturnFilter filter = ReturnFilter.all();
        assertNull(filter.orderId());
        assertNull(filter.buyerLogin());
        assertNull(filter.buyerEmail());
        assertNull(filter.referenceNumber());
        assertNull(filter.createdFrom());
        assertNull(filter.createdTo());
    }

    @Test
    void returnFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        ReturnFilter filter = ReturnFilter.builder()
                .orderId(ORDER_ID).buyerLogin(BUYER_LOGIN).buyerEmail(BUYER_EMAIL)
                .referenceNumber(REFERENCE_NUMBER).createdFrom(FROM).createdTo(TO)
                .build();

        // then
        assertEquals(ORDER_ID, filter.orderId());
        assertEquals(BUYER_EMAIL, filter.buyerEmail());
        assertEquals(REFERENCE_NUMBER, filter.referenceNumber());
        ReturnFilter copy = filter.toBuilder().build();
        assertEquals(BUYER_LOGIN, copy.buyerLogin());
        assertEquals(FROM, copy.createdFrom());
        assertEquals(TO, copy.createdTo());
    }

    @Test
    void claimFilter_whenAll_isEmpty() {
        // then
        ClaimFilter filter = ClaimFilter.all();
        assertNull(filter.offerId());
        assertNull(filter.buyerId());
        assertNull(filter.status());
    }

    @Test
    void claimFilter_whenAllFieldsSet_buildsAndToBuilderPreserves() {
        // when
        ClaimFilter filter = ClaimFilter.builder()
                .offerId(OFFER_ID).buyerId(BUYER_ID).status(STATUS).build();

        // then
        assertEquals(OFFER_ID, filter.offerId());
        assertEquals(BUYER_ID, filter.buyerId());
        assertEquals(STATUS, filter.status());
        ClaimFilter copy = filter.toBuilder().build();
        assertEquals(OFFER_ID, copy.offerId());
        assertEquals(BUYER_ID, copy.buyerId());
        assertEquals(STATUS, copy.status());
    }
}
