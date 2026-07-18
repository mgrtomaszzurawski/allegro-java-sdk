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
    /** User's additional e-mail addresses ({@code /account/additional-emails}). */
    public static final String ADDITIONAL_EMAILS = "/account/additional-emails";
    /** Seller sales-quality report ({@code /sale/quality}). */
    public static final String SALES_QUALITY = "/sale/quality";
    /** Smart! seller classification report ({@code /sale/smart}). */
    public static final String SMART_CLASSIFICATION = "/sale/smart";
    /** The authenticated user's received ratings ({@code /sale/user-ratings}). */
    public static final String USER_RATINGS = "/sale/user-ratings";
    /** Public per-user resources root ({@code /users}); build with {@link #subPath}. */
    public static final String USERS = "/users";
    /** Auction bids root ({@code /bidding/offers}); build with {@link #subPath}. */
    public static final String BIDDING_OFFERS = "/bidding/offers";
    /** Charity fundraising-campaign search ({@code /charity/fundraising-campaigns}). */
    public static final String CHARITY_CAMPAIGNS = "/charity/fundraising-campaigns";
    /** CPS affiliate conversions ({@code /affiliate/conversions/cps}). */
    public static final String AFFILIATE_CPS_CONVERSIONS = "/affiliate/conversions/cps";
    /** {@code ratings-summary} sub-resource segment (under {@link #USERS}). */
    public static final String RATINGS_SUMMARY_SEGMENT = "ratings-summary";
    /** {@code bid} sub-resource segment (under {@link #BIDDING_OFFERS}). */
    public static final String BID_SEGMENT = "bid";
    /** {@code answer} sub-resource segment (under {@link #USER_RATINGS}). */
    public static final String ANSWER_SEGMENT = "answer";
    /** {@code removal} sub-resource segment (under {@link #USER_RATINGS}). */
    public static final String REMOVAL_SEGMENT = "removal";

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

    // ---- catalog (bucket E) ----
    /** Category tree; {@code parent.id} filters to one node's direct children. */
    public static final String CATEGORIES = "/sale/categories";
    /** Parameters sub-resource of a category (under {@link #CATEGORIES}/{id}). */
    public static final String CATEGORY_PARAMETERS_SEGMENT = "parameters";
    /** Category suggestions matched by name ({@code /sale/matching-categories}). */
    public static final String MATCHING_CATEGORIES = "/sale/matching-categories";

    // ---- offers-extras + classifieds (bucket F) ----
    /** Classifieds (advertisement) package configurations, filtered by category. */
    public static final String CLASSIFIEDS_PACKAGES = "/sale/classifieds-packages";
    private static final String OFFER_CLASSIFIEDS_PACKAGES = "/sale/offer-classifieds-packages";

    /** One classifieds package configuration ({@code /sale/classifieds-packages/{packageId}}). */
    public static String classifiedsPackage(String packageId) {
        return subPath(CLASSIFIEDS_PACKAGES, packageId);
    }

    /** Packages assigned to an offer ({@code /sale/offer-classifieds-packages/{offerId}}). */
    public static String offerClassifiedsPackages(String offerId) {
        return subPath(OFFER_CLASSIFIEDS_PACKAGES, offerId);
    }

    // ---- pricing (bucket G) ----
    /** Automatic pricing rules collection ({@code /sale/price-automation/rules}). */
    public static final String PRICE_AUTOMATION_RULES = "/sale/price-automation/rules";
    // ---- campaigns (bucket H) ----
    /** Available badge campaigns ({@code /sale/badge-campaigns}). */
    public static final String BADGE_CAMPAIGNS = "/sale/badge-campaigns";

    // ---- shipping (bucket C) ----
    /** Seller's points of service (personal-collection locations). */
    public static final String POINTS_OF_SERVICE = "/points-of-service";
    /** Delivery methods Allegro offers the seller. */
    public static final String DELIVERY_METHODS = "/sale/delivery-methods";

    // ---- fulfillment (bucket I) ----
    /** Seller's active removal preference for One Fulfillment goods. */
    public static final String FULFILLMENT_REMOVAL_PREFERENCES = "/fulfillment/removal/preferences";

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
