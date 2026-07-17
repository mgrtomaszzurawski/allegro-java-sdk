/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

/**
 * Single source of truth for Allegro REST paths. Domain clients build their
 * endpoint URIs exclusively from these constants — never from inline string
 * literals — so a path change is a one-line diff.
 *
 * @since 0.1.0
 */
public final class ApiPaths {

    // ---- account (bucket D) ----
    /** Basic information about the authenticated user ({@code /me}). */
    public static final String CURRENT_USER = "/me";
    /** Details for all marketplaces on the platform ({@code /marketplaces}). */
    public static final String MARKETPLACES = "/marketplaces";

    // ---- offers (bucket A) ----
    private static final String SALE_PRODUCT_OFFERS = "/sale/product-offers";
    private static final String OFFERS = "/offers";
    private static final String CHANGE_PRICE_COMMANDS = "change-price-commands";

    /** Full data of a single product-offer ({@code /sale/product-offers/{offerId}}). */
    public static String productOffer(String offerId) {
        return subPath(SALE_PRODUCT_OFFERS, offerId);
    }

    /** Single-offer Buy Now price change command ({@code /offers/{offerId}/change-price-commands/{commandId}}). */
    public static String changePriceCommand(String offerId, String commandId) {
        return subPath(OFFERS, offerId, CHANGE_PRICE_COMMANDS, commandId);
    }

    // ---- orders (bucket B) ----
    /** Seller's orders collection; append the order id for a single order. */
    public static final String ORDER_CHECKOUT_FORMS = "/order/checkout-forms";

    // ---- offers-extras + classifieds (bucket F) ----
    /** Classifieds (advertisement) package configurations, filtered by category. */
    public static final String CLASSIFIEDS_PACKAGES = "/sale/classifieds-packages";

    // ---- contacts (bucket J) ----
    /** Seller contact cards ({@code /sale/offer-contacts}); append {@code /{id}} via subPath. */
    public static final String OFFER_CONTACTS = "/sale/offer-contacts";

    // [append point: domain paths] Each domain bucket appends its own
    // "---- <feature> (bucket X) ----" section above this marker, one block
    // per bucket, in BACKLOG order.

    private ApiPaths() {
    }

    private static final char PATH_SEPARATOR = '/';
    private static final String ENCODED_SPACE = "%20";
    private static final String CURRENT_DIR_SEGMENT = ".";
    private static final String PARENT_DIR_SEGMENT = "..";
    private static final String ERR_UNSAFE_SEGMENT = "Unsafe path segment: ";

    /**
     * Join a base path with dynamic segments, guaranteeing exactly one
     * {@code /} separator between parts. Segments are percent-encoded, so an
     * identifier containing {@code /}, {@code ?}, {@code #} or {@code ..}
     * cannot re-route the request or smuggle query parameters.
     */
    public static String subPath(String basePath, String... segments) {
        StringBuilder joined = new StringBuilder(basePath);
        for (String segment : segments) {
            if (segment.isEmpty() || CURRENT_DIR_SEGMENT.equals(segment)
                    || PARENT_DIR_SEGMENT.equals(segment)) {
                // URLEncoder leaves dots alone, so ".." would survive encoding
                // and server-side normalization could re-route the call.
                throw new IllegalArgumentException(ERR_UNSAFE_SEGMENT + segment);
            }
            if (joined.charAt(joined.length() - 1) != PATH_SEPARATOR) {
                joined.append(PATH_SEPARATOR);
            }
            joined.append(java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", ENCODED_SPACE));
        }
        return joined.toString();
    }
}
