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
    /** Product-offers collection ({@code /sale/product-offers}); POST to create. */
    public static final String SALE_PRODUCT_OFFERS = "/sale/product-offers";
    private static final String OFFERS = "/offers";
    private static final String CHANGE_PRICE_COMMANDS = "change-price-commands";
    private static final String SMART_SEGMENT = "smart";
    private static final String SALE_OFFER_PUBLICATION_COMMANDS = "/sale/offer-publication-commands";
    private static final String SALE_OFFER_PRICE_CHANGE_COMMANDS = "/sale/offer-price-change-commands";
    private static final String SALE_OFFER_QUANTITY_CHANGE_COMMANDS = "/sale/offer-quantity-change-commands";
    private static final String TASKS_SEGMENT = "tasks";

    /** Seller's offers collection ({@code /sale/offers}); offset/limit paged. */
    public static final String SALE_OFFERS = "/sale/offers";
    /** Seller's offers with missing category parameters ({@code /sale/offers/unfilled-parameters}). */
    public static final String SALE_OFFERS_UNFILLED_PARAMETERS = "/sale/offers/unfilled-parameters";

    /** Full data of a single product-offer ({@code /sale/product-offers/{offerId}}); PATCH to edit. */
    public static String productOffer(String offerId) {
        return subPath(SALE_PRODUCT_OFFERS, offerId);
    }

    /** A single offer as a draft resource ({@code /sale/offers/{offerId}}); DELETE to remove a draft. */
    public static String offerDraft(String offerId) {
        return subPath(SALE_OFFERS, offerId);
    }

    /** Single-offer Buy Now price change command ({@code /offers/{offerId}/change-price-commands/{commandId}}). */
    public static String changePriceCommand(String offerId, String commandId) {
        return subPath(OFFERS, offerId, CHANGE_PRICE_COMMANDS, commandId);
    }

    /** Smart! classification report of one offer ({@code /sale/offers/{offerId}/smart}). */
    public static String offerSmart(String offerId) {
        return subPath(SALE_OFFERS, offerId, SMART_SEGMENT);
    }

    /** Batch publish/unpublish command ({@code /sale/offer-publication-commands/{commandId}}). */
    public static String offerPublicationCommand(String commandId) {
        return subPath(SALE_OFFER_PUBLICATION_COMMANDS, commandId);
    }

    /** Per-offer tasks of a publish/unpublish command ({@code …/{commandId}/tasks}). */
    public static String offerPublicationCommandTasks(String commandId) {
        return subPath(SALE_OFFER_PUBLICATION_COMMANDS, commandId, TASKS_SEGMENT);
    }

    /** Batch price-change command ({@code /sale/offer-price-change-commands/{commandId}}). */
    public static String offerPriceChangeCommand(String commandId) {
        return subPath(SALE_OFFER_PRICE_CHANGE_COMMANDS, commandId);
    }

    /** Per-offer tasks of a price-change command ({@code …/{commandId}/tasks}). */
    public static String offerPriceChangeCommandTasks(String commandId) {
        return subPath(SALE_OFFER_PRICE_CHANGE_COMMANDS, commandId, TASKS_SEGMENT);
    }

    /** Batch quantity-change command ({@code /sale/offer-quantity-change-commands/{commandId}}). */
    public static String offerQuantityChangeCommand(String commandId) {
        return subPath(SALE_OFFER_QUANTITY_CHANGE_COMMANDS, commandId);
    }

    /** Per-offer tasks of a quantity-change command ({@code …/{commandId}/tasks}). */
    public static String offerQuantityChangeCommandTasks(String commandId) {
        return subPath(SALE_OFFER_QUANTITY_CHANGE_COMMANDS, commandId, TASKS_SEGMENT);
    }

    // ---- orders (bucket B) ----
    /** Order resources root ({@code /order}); build sub-resources with {@link #subPath}. */
    public static final String ORDER_ROOT = "/order";
    /** Seller's orders collection; append the order id for a single order. */
    public static final String ORDER_CHECKOUT_FORMS = "/order/checkout-forms";
    /** Seller's order event log ({@code /order/events}); cursor-paged by {@code from}. */
    public static final String ORDER_EVENTS = "/order/events";
    /** Latest-order-event marker ({@code /order/event-stats}). */
    public static final String ORDER_EVENT_STATS = "/order/event-stats";
    /** Available shipping carriers dictionary ({@code /order/carriers}). */
    public static final String ORDER_CARRIERS = "/order/carriers";
    /** Allegro pickup/drop-off points ({@code /order/carriers/ALLEGRO/points}). */
    public static final String ALLEGRO_PICKUP_POINTS = "/order/carriers/ALLEGRO/points";
    /** {@code fulfillment} sub-resource segment (under an order). */
    public static final String FULFILLMENT_SEGMENT = "fulfillment";
    /** {@code serial-numbers} sub-resource segment (under an order). */
    public static final String SERIAL_NUMBERS_SEGMENT = "serial-numbers";
    /** {@code shipments} sub-resource segment (under an order). */
    public static final String SHIPMENTS_SEGMENT = "shipments";
    /** {@code tracking} sub-resource segment (under a carrier). */
    public static final String TRACKING_SEGMENT = "tracking";
    /** {@code billing-documents} sub-resource segment (under an order). */
    public static final String BILLING_DOCUMENTS_SEGMENT = "billing-documents";
    /** {@code links} sub-resource segment (under an order's billing documents). */
    public static final String LINKS_SEGMENT = "links";
    /** Seller payment operations history ({@code /payments/payment-operations}). */
    public static final String PAYMENT_OPERATIONS = "/payments/payment-operations";
    /** Refunded payments / refund initiation ({@code /payments/refunds}). */
    public static final String PAYMENT_REFUNDS = "/payments/refunds";
    /** Seller billing entries ({@code /billing/billing-entries}). */
    public static final String BILLING_ENTRIES = "/billing/billing-entries";
    /** Billing type dictionary ({@code /billing/billing-types}). */
    public static final String BILLING_TYPES = "/billing/billing-types";

    // ---- catalog (bucket E) ----
    /** Category tree; {@code parent.id} filters to one node's direct children. */
    public static final String CATEGORIES = "/sale/categories";
    /** Parameters sub-resource of a category (under {@link #CATEGORIES}/{id}). */
    public static final String CATEGORY_PARAMETERS_SEGMENT = "parameters";
    /** Category suggestions matched by name ({@code /sale/matching-categories}). */
    public static final String MATCHING_CATEGORIES = "/sale/matching-categories";
    /** Product database search + read ({@code /sale/products}); append {@code /{id}} via subPath. */
    public static final String PRODUCTS = "/sale/products";

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

    /** Daily advertisement statistics for selected offers ({@code offer.id} query). */
    public static final String CLASSIFIED_OFFERS_STATS = "/sale/classified-offers-stats";
    /** Daily advertisement statistics aggregated for the seller. */
    public static final String CLASSIFIED_SELLER_STATS = "/sale/classified-seller-stats";
    /** Seller's private offer tags ({@code /sale/offer-tags}); append {@code /{tagId}}. */
    public static final String OFFER_TAGS = "/sale/offer-tags";
    private static final String TAGS_SEGMENT = "tags";

    /** One offer tag ({@code /sale/offer-tags/{tagId}}). */
    public static String offerTag(String tagId) {
        return subPath(OFFER_TAGS, tagId);
    }

    /** Tags assigned to an offer ({@code /sale/offers/{offerId}/tags}). */
    public static String offerAssignedTags(String offerId) {
        return subPath(SALE_OFFERS, offerId, TAGS_SEGMENT);
    }

    private static final String TRANSLATIONS_SEGMENT = "translations";
    private static final String RATING_SEGMENT = "rating";

    /** An offer's translations ({@code /sale/offers/{offerId}/translations}). */
    public static String offerTranslations(String offerId) {
        return subPath(SALE_OFFERS, offerId, TRANSLATIONS_SEGMENT);
    }

    /** One offer translation ({@code /sale/offers/{offerId}/translations/{language}}). */
    public static String offerTranslation(String offerId, String language) {
        return subPath(SALE_OFFERS, offerId, TRANSLATIONS_SEGMENT, language);
    }

    /** An offer's buyer rating ({@code /sale/offers/{offerId}/rating}). */
    public static String offerRating(String offerId) {
        return subPath(SALE_OFFERS, offerId, RATING_SEGMENT);
    }

    /** Seller's fixed offer bundles ({@code /sale/bundles}); cursor-paged by {@code page.id}. */
    public static final String BUNDLES = "/sale/bundles";
    private static final String DISCOUNT_SEGMENT = "discount";

    /** One offer bundle ({@code /sale/bundles/{bundleId}}). */
    public static String bundle(String bundleId) {
        return subPath(BUNDLES, bundleId);
    }

    /** A bundle's discount ({@code /sale/bundles/{bundleId}/discount}). */
    public static String bundleDiscount(String bundleId) {
        return subPath(BUNDLES, bundleId, DISCOUNT_SEGMENT);
    }

    // ---- pricing (bucket G) ----
    /** Automatic pricing rules collection ({@code /sale/price-automation/rules}). */
    public static final String PRICE_AUTOMATION_RULES = "/sale/price-automation/rules";
    /** Fee-and-commission preview for a draft offer ({@code /pricing/offer-fee-preview}). */
    public static final String OFFER_FEE_PREVIEW = "/pricing/offer-fee-preview";
    /** The user's current offer fee quotes ({@code /pricing/offer-quotes}). */
    public static final String OFFER_QUOTES = "/pricing/offer-quotes";
    /** Turnover-discount configuration ({@code /sale/turnover-discount}); append {@code /{marketplaceId}} via {@link #subPath}. */
    public static final String TURNOVER_DISCOUNT = "/sale/turnover-discount";
    /** Available deposit types ({@code /deposit/types}). */
    public static final String DEPOSIT_TYPES = "/deposit/types";

    private static final String PRICE_AUTOMATION_OFFERS = "/sale/price-automation/offers";
    private static final String RULES_SEGMENT = "rules";
    private static final String DEACTIVATE_SEGMENT = "deactivate";

    /** Automatic pricing rules assigned to one offer ({@code /sale/price-automation/offers/{offerId}/rules}). */
    public static String priceAutomationOfferRules(String offerId) {
        return subPath(PRICE_AUTOMATION_OFFERS, offerId, RULES_SEGMENT);
    }

    /** Deactivate a marketplace's turnover discount ({@code /sale/turnover-discount/{marketplaceId}/deactivate}). */
    public static String turnoverDiscountDeactivate(String marketplaceId) {
        return subPath(TURNOVER_DISCOUNT, marketplaceId, DEACTIVATE_SEGMENT);
    }

    // ---- campaigns (bucket H) ----
    /** Available badge campaigns ({@code /sale/badge-campaigns}). */
    public static final String BADGE_CAMPAIGNS = "/sale/badge-campaigns";
    /** Badges collection ({@code /sale/badges}); apply and list; build sub-paths via {@link #subPath}. */
    public static final String BADGES = "/sale/badges";
    /** Badge applications ({@code /sale/badge-applications}); append {@code /{applicationId}} via subPath. */
    public static final String BADGE_APPLICATIONS = "/sale/badge-applications";
    /** Badge operations ({@code /sale/badge-operations}); append {@code /{operationId}} via subPath. */
    public static final String BADGE_OPERATIONS = "/sale/badge-operations";
    /** Path segment for an offer within the badge update path. */
    public static final String BADGE_OFFERS_SEGMENT = "offers";
    /** Path segment for a campaign within the badge update path. */
    public static final String BADGE_CAMPAIGNS_SEGMENT = "campaigns";
    /** Allegro Prices account participation ({@code /sale/allegro-prices/accounts/participations}). */
    public static final String ALLEGRO_PRICES_PARTICIPATIONS =
            "/sale/allegro-prices/accounts/participations";
    /** Allegro Prices offer-status query ({@code /sale/allegro-prices/offers-queries}). */
    public static final String ALLEGRO_PRICES_OFFERS_QUERIES = "/sale/allegro-prices/offers-queries";
    /** Allegro Prices submit-offer commands; append {@code /{commandId}} via subPath. */
    public static final String ALLEGRO_PRICES_SUBMIT_COMMANDS =
            "/sale/allegro-prices/offers/submit-offer-commands";
    /** Allegro Prices exclusion commands; append {@code /{commandId}} via subPath. */
    public static final String ALLEGRO_PRICES_EXCLUSION_COMMANDS =
            "/sale/allegro-prices/offers/exclusion-commands";
    /** AlleDiscount campaigns list ({@code /sale/alle-discount/campaigns}). */
    public static final String ALLE_DISCOUNT_CAMPAIGNS = "/sale/alle-discount/campaigns";
    /** AlleDiscount root ({@code /sale/alle-discount}); build per-campaign sub-paths via subPath. */
    public static final String ALLE_DISCOUNT = "/sale/alle-discount";
    /** AlleDiscount submit-offer commands; append {@code /{commandId}} via subPath. */
    public static final String ALLE_DISCOUNT_SUBMIT_COMMANDS = "/sale/alle-discount/submit-offer-commands";
    /** AlleDiscount withdraw-offer commands; append {@code /{commandId}} via subPath. */
    public static final String ALLE_DISCOUNT_WITHDRAW_COMMANDS =
            "/sale/alle-discount/withdraw-offer-commands";
    /** Path segment for a campaign's eligible offers ({@code …/{campaignId}/eligible-offers}). */
    public static final String ALLE_DISCOUNT_ELIGIBLE_OFFERS_SEGMENT = "eligible-offers";
    /** Path segment for a campaign's submitted offers ({@code …/{campaignId}/submitted-offers}). */
    public static final String ALLE_DISCOUNT_SUBMITTED_OFFERS_SEGMENT = "submitted-offers";

    // ---- shipping (bucket C) ----
    /** Seller's points of service (personal-collection locations). */
    public static final String POINTS_OF_SERVICE = "/points-of-service";
    /** Delivery methods Allegro offers the seller. */
    public static final String DELIVERY_METHODS = "/sale/delivery-methods";

    // ---- fulfillment (bucket I) ----
    /** Seller's active removal preference for One Fulfillment goods. */
    public static final String FULFILLMENT_REMOVAL_PREFERENCES = "/fulfillment/removal/preferences";
    /** Available stock report for One Fulfillment goods. */
    public static final String FULFILLMENT_STOCK = "/fulfillment/stock";
    /** Products the seller may ship into One Fulfillment. */
    public static final String FULFILLMENT_AVAILABLE_PRODUCTS = "/fulfillment/available-products";
    /** Refund-dispositions report for returned/bounced One Fulfillment goods. */
    public static final String FULFILLMENT_REFUND_DISPOSITIONS = "/fulfillment/returns/refund-dispositions";
    /** Seller's tax identification number for One Fulfillment. */
    public static final String FULFILLMENT_TAX_ID = "/fulfillment/tax-id";
    private static final String FULFILLMENT_ORDERS = "/fulfillment/orders";
    private static final String PARCELS_SEGMENT = "parcels";

    /** Parcels shipped for one fulfillment order ({@code /fulfillment/orders/{orderId}/parcels}). */
    public static String fulfillmentOrderParcels(String orderId) {
        return subPath(FULFILLMENT_ORDERS, orderId, PARCELS_SEGMENT);
    }

    // ---- contacts (bucket J) ----
    /** Seller contact cards ({@code /sale/offer-contacts}); append {@code /{id}} via subPath. */
    public static final String OFFER_CONTACTS = "/sale/offer-contacts";

    // ---- messaging (bucket J) ----
    /** Message-center threads ({@code /messaging/threads}); append {@code /{threadId}} via subPath. */
    public static final String MESSAGING_THREADS = "/messaging/threads";
    /** Single messages ({@code /messaging/messages}); append {@code /{messageId}} via subPath. */
    public static final String MESSAGING_MESSAGES = "/messaging/messages";
    /** Message attachments ({@code /messaging/message-attachments}); append {@code /{attachmentId}} via subPath. */
    public static final String MESSAGING_MESSAGE_ATTACHMENTS = "/messaging/message-attachments";
    /** Sub-resource: the messages collection within a thread. */
    public static final String MESSAGES_SEGMENT = "messages";
    /** Sub-resource: the read-flag toggle of a thread. */
    public static final String READ_SEGMENT = "read";

    // ---- disputes / post-purchase issues (bucket J) ----
    /** Post-purchase issues ({@code /sale/issues}); append {@code /{issueId}} via subPath. */
    public static final String ISSUES = "/sale/issues";
    /** Sub-resource: the chat within an issue. */
    public static final String CHAT_SEGMENT = "chat";

    // ---- sale-settings (bucket K) ----
    /** Seller after-sale warranty definitions. */
    public static final String AFTER_SALES_WARRANTIES =
            "/after-sales-service-conditions/warranties";

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
