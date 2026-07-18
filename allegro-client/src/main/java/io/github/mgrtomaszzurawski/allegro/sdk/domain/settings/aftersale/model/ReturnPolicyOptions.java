/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyOptionsRaw;
import org.jspecify.annotations.Nullable;

/**
 * The boolean return-handling options of a return policy.
 *
 * @param cashOnDeliveryNotAllowed a returned order may not be sent back cash-on-delivery
 * @param freeAccessoriesReturnRequired free accessories added to the order must be returned too
 * @param refundLoweredByReceivedDiscount a post-order discount lowers the refund by that amount
 * @param businessReturnAllowed returns are allowed for B2B purchases
 * @param collectBySellerOnly returned items are collected by the seller
 *
 * @since 0.3.0
 */
public record ReturnPolicyOptions(
        boolean cashOnDeliveryNotAllowed,
        boolean freeAccessoriesReturnRequired,
        boolean refundLoweredByReceivedDiscount,
        boolean businessReturnAllowed,
        boolean collectBySellerOnly) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ReturnPolicyOptions from(@Nullable ReturnPolicyOptionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ReturnPolicyOptions(
                Boolean.TRUE.equals(raw.getCashOnDeliveryNotAllowed()),
                Boolean.TRUE.equals(raw.getFreeAccessoriesReturnRequired()),
                Boolean.TRUE.equals(raw.getRefundLoweredByReceivedDiscount()),
                Boolean.TRUE.equals(raw.getBusinessReturnAllowed()),
                Boolean.TRUE.equals(raw.getCollectBySellerOnly()));
    }
}
