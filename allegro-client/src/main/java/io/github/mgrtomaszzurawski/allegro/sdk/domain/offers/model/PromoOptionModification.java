/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsModificationRaw;
import java.util.Objects;

/**
 * One change to an offer's promotion packages — set/{@code change} a package, or remove one
 * ({@code now}, or at the end of its billing cycle). Combine several to
 * {@code offers().promoOptions().modify(offerId, …)}.
 *
 * @param kind        what to do (change / remove now / remove at end of cycle)
 * @param packageType which slot the change targets (base or extra)
 * @param packageId   the promotion package id (from {@code availablePackages()})
 * @since 0.4.0
 */
public record PromoOptionModification(Kind kind, PromoPackageType packageType, String packageId) {

    /** The kind of promo-options change. */
    public enum Kind {
        /** Set or change the package in the slot. */
        CHANGE("CHANGE"),
        /** Remove the package immediately. */
        REMOVE_NOW("REMOVE_NOW"),
        /** Remove the package at the end of its current billing cycle. */
        REMOVE_AT_END_OF_CYCLE("REMOVE_WITH_END_OF_CYCLE");

        private final String wireValue;

        Kind(String wireValue) {
            this.wireValue = wireValue;
        }

        String wireValue() {
            return wireValue;
        }
    }

    /** Canonical constructor: all fields are required. */
    public PromoOptionModification {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(packageType, "packageType");
        Objects.requireNonNull(packageId, "packageId");
    }

    /** Set or change the given package in its slot. */
    public static PromoOptionModification change(PromoPackageType packageType, String packageId) {
        return new PromoOptionModification(Kind.CHANGE, packageType, packageId);
    }

    /** Remove the given package immediately. */
    public static PromoOptionModification removeNow(PromoPackageType packageType, String packageId) {
        return new PromoOptionModification(Kind.REMOVE_NOW, packageType, packageId);
    }

    /** Remove the given package at the end of its current billing cycle. */
    public static PromoOptionModification removeAtEndOfCycle(PromoPackageType packageType, String packageId) {
        return new PromoOptionModification(Kind.REMOVE_AT_END_OF_CYCLE, packageType, packageId);
    }

    /** The generated request modification for this change. */
    public PromoOptionsModificationRaw toRaw() {
        return new PromoOptionsModificationRaw()
                .modificationType(PromoOptionsModificationRaw.ModificationTypeEnum.fromValue(kind.wireValue()))
                .packageType(PromoOptionsModificationRaw.PackageTypeEnum.fromValue(packageType.name()))
                .packageId(packageId);
    }
}
