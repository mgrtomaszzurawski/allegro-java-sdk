/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

/**
 * What Allegro should do with a seller's goods that must leave the One
 * Fulfillment warehouse.
 *
 * @since 0.2.0
 */
public enum RemovalOperation {

    /** Return the goods to the seller (a return address is required). */
    WITHDRAWAL("WITHDRAWAL"),

    /** Dispose of the goods at the warehouse. */
    DISPOSAL("DISPOSAL");

    private static final String ERR_UNKNOWN = "Unknown removal operation: ";

    private final String wireValue;

    RemovalOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact token Allegro uses on the wire for this operation. */
    public String wireValue() {
        return wireValue;
    }

    /** Resolve the enum from the wire token; the set is closed, so an unknown
     * token is a contract drift and fails loudly rather than being swallowed. */
    public static RemovalOperation fromWire(String wireValue) {
        for (RemovalOperation operation : values()) {
            if (operation.wireValue.equals(wireValue)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(ERR_UNKNOWN + wireValue);
    }
}
