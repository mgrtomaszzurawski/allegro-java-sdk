# API-SURFACE.md — the consumer-facing method layout (PROPOSED)

How a consumer navigates the SDK from second one: one client, domain accessors, intent-named
methods. Same navigation model as ksef-java-sdk (`client.invoices().archive().getBy…`).
Builders are omitted here by design — every `*Request` parameter below has a fluent builder.

**Status: PROPOSED baseline.** Each bucket owner refines their facade in the starter-slice PR
(review may rename); the ROOT accessor names, the sub-facade split, and the endpoint→method
mapping below are the consistency contract. `Stream<T>` = lazy pagination. *(sync)* = async
command endpoint wrapped sync-default: one call submits and polls to a terminal state
internally; an optional `Duration timeout` overload exists; the command-status endpoints are
consumed internally and do not appear as public methods.

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {

    client.offers()        // A  selling: the offer lifecycle
    client.orders()        // B  what customers bought
    client.payments()      // B  money flow + refunds
    client.billing()       // B  marketplace fees
    client.shipping()      // C  parcels, labels, pickups, delivery config
    client.user()          // D  the authenticated account
    client.bidding()       // D  auctions (buyer side)
    client.marketplaces()  // D  allegro.pl / allegro.cz / …
    client.charity()       // D  fundraising campaigns
    client.affiliate()     // D  affiliate conversions
    client.catalog()       // E  categories, products, compatibility
    client.classifieds()   // F  advertisement packages + stats
    client.pricing()       // G  fees, automation rules, promotions
    client.campaigns()     // H  badges, Allegro Prices, AlleDiscount
    client.fulfillment()   // I  One Fulfillment by Allegro
    client.messaging()     // J  buyer-seller message center
    client.disputes()      // J  post-purchase issues
    client.contacts()      // J  seller contact cards
    client.settings()      // K  account-level sale configuration
}
```

---

## A — `client.offers()` (root facade: bucket A; sub-accessor lines appended by F)

```java
Offers offers = client.offers();

// The offer lifecycle — /sale/product-offers, /sale/offers
offers.create(CreateOfferRequest)                 // POST  /sale/product-offers   (sync: polls operations/{id})
offers.edit(offerId, EditOfferRequest)            // PATCH /sale/product-offers/{offerId}   (sync)
offers.get(offerId)                               // GET   /sale/product-offers/{offerId}
offers.getFields(offerId, OfferPart...)           // GET   /sale/product-offers/{offerId}/parts
offers.deleteDraft(offerId)                       // DELETE /sale/offers/{offerId}
offers.streamOffers(OfferFilter)                  // GET   /sale/offers                     Stream<OfferSummary>
offers.streamEvents(OfferEventFilter)             // GET   /sale/offer-events               Stream<OfferEvent>
offers.operationStatus(offerId, operationId)      // GET   /sale/product-offers/{offerId}/operations/{operationId}
offers.smartClassification(offerId)               // GET   /sale/offers/{offerId}/smart
offers.streamUnfilledParameters()                 // GET   /sale/offers/unfilled-parameters Stream<…>
offers.changeBuyNowPrice(offerId, newPrice)       // PUT   /offers/{offerId}/change-price-commands/{commandId} (sync)

// Bulk operations — command queues, sync-default, one report back
OfferBatch batch = offers.batch();
batch.modify(BatchModificationRequest)            // PUT  /sale/offer-modification-commands/{id}        (sync)
batch.changePrices(BatchPriceChangeRequest)       // PUT  /sale/offer-price-change-commands/{id}        (sync)
batch.changeQuantities(BatchQuantityChangeRequest)// PUT  /sale/offer-quantity-change-commands/{id}     (sync)
batch.publish(offerIds...)                        // PUT  /sale/offer-publication-commands/{id}         (sync)
batch.unpublish(offerIds...)                      // PUT  /sale/offer-publication-commands/{id}         (sync)
batch.modifyPricesAndStock(BulkModificationRequest) // POST /sale/offer-bulk-modification-commands      (sync, beta)
batch.applyPricingRules(BatchPricingRulesRequest) // POST /sale/offer-price-automation-commands         (sync)
// (each returns BatchReport with per-offer task results — GET …-commands/{id} + /tasks consumed internally)

// Promotion packages for offers
OfferPromoOptions promo = offers.promoOptions();
promo.availablePackages()                         // GET  /sale/offer-promotion-packages
promo.forAllOffers()                              // GET  /sale/offers/promo-options
promo.forOffer(offerId)                           // GET  /sale/offers/{offerId}/promo-options
promo.modify(offerId, List<PromoOptionModification>) // POST /sale/offers/{offerId}/promo-options-modification
promo.modifyBatch(BatchPromoOptionsRequest)       // PUT  /sale/offers/promo-options-commands/{id}          (sync)

// Images and attachments
OfferMedia media = offers.media();
media.uploadImage(bytes | url)                    // POST /sale/images
media.createAttachment(AttachmentDeclaration)     // POST /sale/offer-attachments
media.uploadAttachment(declared, bytes, contentType) // PUT to the declaration's one-time Location URL
media.getAttachment(attachmentId)                 // GET  /sale/offer-attachments/{attachmentId}
```

## F — appended onto `client.offers()` (owner: bucket F) + `client.classifieds()`

```java
// Tags
OfferTags tags = offers.tags();
tags.streamTags()                                 // GET    /sale/offer-tags        Stream<Tag>
tags.create(TagRequest)                           // POST   /sale/offer-tags
tags.rename(tagId, TagRequest)                    // PUT    /sale/offer-tags/{tagId}
tags.delete(tagId)                                // DELETE /sale/offer-tags/{tagId}
tags.assignToOffer(offerId, tagIds)               // POST   /sale/offers/{offerId}/tags
tags.ofOffer(offerId)                             // GET    /sale/offers/{offerId}/tags

// Translations
OfferTranslations translations = offers.translations();
translations.ofOffer(offerId)                     // GET    /sale/offers/{offerId}/translations
translations.update(offerId, language, TranslationRequest) // PATCH /sale/offers/{offerId}/translations/{language} (title + description + safetyInformation; partial)
translations.delete(offerId, language)            // DELETE /sale/offers/{offerId}/translations/{language}

// Rating
offers.rating(offerId)                            // GET    /sale/offers/{offerId}/rating

// Fixed bundles
OfferBundles bundles = offers.bundles();
bundles.streamBundles()                           // GET    /sale/bundles            Stream<OfferBundle> (cursor)
bundles.get(bundleId)                             // GET    /sale/bundles/{bundleId}
bundles.updateDiscount(bundleId, List<BundleDiscount>) // PUT /sale/bundles/{bundleId}/discount
bundles.delete(bundleId)                          // DELETE /sale/bundles/{bundleId}

// Flexible bundles
FlexibleBundles flexible = offers.flexibleBundles();
flexible.streamBundles()                          // GET    /sale/flexible-bundles   Stream<FlexibleBundleSummary> (cursor)
flexible.get(bundleId)                            // GET    /sale/flexible-bundles/{bundleId}  -> FlexibleBundle
flexible.create(FlexibleBundleRequest)            // POST   /sale/flexible-bundles            -> FlexibleBundle
flexible.update(bundleId, FlexibleBundleRequest)  // PUT    /sale/flexible-bundles/{bundleId} -> FlexibleBundle
flexible.delete(bundleId)                         // DELETE /sale/flexible-bundles/{bundleId}

// Classifieds (advertisements) — own top-level accessor
Classifieds classifieds = client.classifieds();
classifieds.availablePackages(categoryId)         // GET /sale/classifieds-packages (category.id required)
classifieds.getPackage(packageId)                 // GET /sale/classifieds-packages/{packageId}
classifieds.packagesOfOffer(offerId)              // GET /sale/offer-classifieds-packages/{offerId}
classifieds.assignPackages(offerId, ClassifiedAssignment) // PUT /sale/offer-classifieds-packages/{offerId}
classifieds.offerStats(offerIds, ClassifiedStatsFilter)  // GET /sale/classified-offers-stats (offer.id array, ≤50)
classifieds.sellerStats(ClassifiedStatsFilter)           // GET /sale/classified-seller-stats
```

## B — `client.orders()`, `client.payments()`, `client.billing()`

```java
Orders orders = client.orders();

orders.streamOrders(OrderFilter)                  // GET /order/checkout-forms        Stream<OrderSummary>
orders.get(orderId)                               // GET /order/checkout-forms/{id}
orders.streamEvents(OrderEventFilter)             // GET /order/events                Stream<OrderEvent>
orders.eventStats()                               // GET /order/event-stats
orders.markStatus(orderId, SellerStatus)          // PUT /order/checkout-forms/{id}/fulfillment
orders.setSerialNumbers(orderId, SerialNumbersRequest) // POST /order/checkout-forms/{id}/serial-numbers
orders.attachBillingDocumentLink(orderId, url)    // POST /order/{orderId}/billing-documents/links

// Parcel tracking on an order
orders.trackingNumbers(orderId)                   // GET  /order/checkout-forms/{id}/shipments
orders.addTrackingNumber(orderId, ShipmentRequest)// POST /order/checkout-forms/{id}/shipments
orders.carriers()                                 // GET  /order/carriers
orders.carrierTracking(carrierId, waybill)        // GET  /order/carriers/{carrierId}/tracking
orders.allegroPickupPoints(PointsFilter)          // GET  /order/carriers/ALLEGRO/points

// Customer invoices on an order
OrderInvoices invoices = orders.invoices();
invoices.ofOrder(orderId)                         // GET  /order/checkout-forms/{id}/invoices
invoices.declare(orderId, InvoiceDeclaration)     // POST /order/checkout-forms/{id}/invoices
invoices.uploadFile(orderId, invoiceId, pdfBytes) // PUT  /order/checkout-forms/{id}/invoices/{invoiceId}/file

// Customer returns (beta)
CustomerReturns returns = orders.returns();
returns.streamReturns(ReturnFilter)               // GET  /order/customer-returns     Stream<CustomerReturn>
returns.get(customerReturnId)                     // GET  /order/customer-returns/{id}
returns.rejectRefund(customerReturnId, RejectionRequest) // POST /order/customer-returns/{id}/rejection

// Commission refund claims
CommissionRefunds commissionRefunds = orders.commissionRefunds();
commissionRefunds.streamClaims(ClaimFilter)       // GET    /order/refund-claims      Stream<RefundClaim>
commissionRefunds.get(claimId)                    // GET    /order/refund-claims/{claimId}
commissionRefunds.claim(RefundClaimRequest)       // POST   /order/refund-claims
commissionRefunds.cancel(claimId)                 // DELETE /order/refund-claims/{claimId}

Payments payments = client.payments();
payments.streamOperations(PaymentOperationFilter) // GET  /payments/payment-operations Stream<PaymentOperation>
payments.streamRefunds(RefundFilter)              // GET  /payments/refunds            Stream<PaymentRefund>
payments.refund(RefundRequest)                    // POST /payments/refunds

Billing billing = client.billing();
billing.streamEntries(BillingFilter)              // GET  /billing/billing-entries     Stream<BillingEntry>
billing.types()                                   // GET  /billing/billing-types
```

## C — `client.shipping()`

```java
Shipping shipping = client.shipping();

// Parcels (WZA — shipment management)
shipping.createShipment(ShipmentRequest)          // POST /shipment-management/shipments/create-commands (sync)
shipping.getShipment(shipmentId)                  // GET  /shipment-management/shipments/{shipmentId}
shipping.cancelShipment(shipmentId)               // POST /shipment-management/shipments/cancel-commands (sync)
shipping.labels(LabelRequest)                     // POST /shipment-management/label            byte[] PDF/ZPL (body: shipmentIds + pageSize/cutLine/summaryReport)
shipping.protocol(shipmentIds...)                 // POST /shipment-management/protocol         byte[] PDF
shipping.requestPickup(PickupRequest)             // POST /shipment-management/pickups/create-commands (sync)
shipping.getPickup(pickupId)                      // GET  /shipment-management/pickups/{pickupId}
shipping.pickupProposals(PickupProposalsRequest)  // POST /shipment-management/pickup-proposals
shipping.deliveryServices()                       // GET  /shipment-management/delivery-services
shipping.deliveryOptionsFor(orderId)              // GET  /shipment-management/delivery-proposals/{orderId}

// Seller delivery configuration
shipping.deliveryMethods()                        // GET  /sale/delivery-methods
DeliverySettings settings = shipping.settings();
settings.get()                                    // GET  /sale/delivery-settings
settings.update(DeliverySettingsRequest)          // PUT  /sale/delivery-settings

ShippingRates rates = shipping.rates();
rates.list()                                      // GET  /sale/shipping-rates          List<ShippingRateSetSummary>  (not paginated; rows carry no rate detail)
rates.get(rateSetId)                              // GET  /sale/shipping-rates/{id}      ShippingRateSet (full, with rate rows)
rates.create(ShippingRateSetRequest)              // POST /sale/shipping-rates
rates.update(rateSetId, ShippingRateSetRequest)   // PUT  /sale/shipping-rates/{id}

PointsOfService points = shipping.points();
points.list()                                     // GET    /points-of-service
points.get(pointId)                               // GET    /points-of-service/{id}
points.create(PointOfServiceRequest)              // POST   /points-of-service
points.update(pointId, PointOfServiceRequest)     // PUT    /points-of-service/{id}
points.delete(pointId)                            // DELETE /points-of-service/{id}
```

## D — `client.user()`, `client.bidding()`, `client.marketplaces()`, `client.charity()`, `client.affiliate()`

```java
UserAccount user = client.user();
user.me()                                         // GET /me                                  (SHIPPED — core slice)
user.salesQuality()                               // GET /sale/quality
user.smartClassification()                        // GET /sale/smart

UserRatings ratings = user.ratings();
ratings.streamRatings(RatingFilter)               // GET /sale/user-ratings          Stream<UserRating>
ratings.get(ratingId)                             // GET /sale/user-ratings/{ratingId}
ratings.answer(ratingId, AnswerRequest)           // PUT /sale/user-ratings/{ratingId}/answer
ratings.requestRemoval(ratingId, RemovalRequest)  // PUT /sale/user-ratings/{ratingId}/removal
ratings.summaryOf(userId)                         // GET /users/{userId}/ratings-summary   (any user, public)

AdditionalEmails emails = user.additionalEmails();
emails.list()                                     // GET    /account/additional-emails
emails.get(emailId)                               // GET    /account/additional-emails/{emailId}
emails.add(emailAddress)                          // POST   /account/additional-emails
emails.delete(emailId)                            // DELETE /account/additional-emails/{emailId}

Bidding bidding = client.bidding();               // works with a BUYER user-token
bidding.myBid(offerId)                            // GET /bidding/offers/{offerId}/bid
bidding.placeBid(offerId, maxAmount)              // PUT /bidding/offers/{offerId}/bid

client.marketplaces().list()                      // GET /marketplaces
client.charity().searchCampaigns(CharityFilter)   // GET /charity/fundraising-campaigns
client.affiliate().streamCpsConversions(Filter)   // GET /affiliate/conversions/cps   Stream<…> (beta)
```

## E — `client.catalog()`

```java
Catalog catalog = client.catalog();

CatalogCategories categories = catalog.categories();
categories.roots() / categories.childrenOf(categoryId) // GET /sale/categories[?parent.id=]
categories.get(categoryId)                        // GET /sale/categories/{categoryId}
categories.parameters(categoryId)                 // GET /sale/categories/{categoryId}/parameters
categories.suggest(productName)                   // GET /sale/matching-categories
categories.streamChanges(since)                   // GET /sale/category-events        Stream<CategoryEvent>
categories.scheduledParameterChanges()            // GET /sale/category-parameters-scheduled-changes

CatalogProducts products = catalog.products();
products.search(ProductSearchRequest)             // GET  /sale/products              Stream<ProductSummary>
products.get(productId)                           // GET  /sale/products/{productId}
products.parametersIn(categoryId)                 // GET  /sale/categories/{categoryId}/product-parameters
products.propose(ProductProposal)                 // POST /sale/product-proposals
products.proposeChange(productId, ChangeProposal) // POST /sale/products/{productId}/change-proposals
products.changeProposal(changeProposalId)         // GET  /sale/products/change-proposals/{changeProposalId}

Compatibility compatibility = catalog.compatibility();
compatibility.supportedCategories()               // GET /sale/compatibility-list/supported-categories
compatibility.suggestionsFor(offerId | productId) // GET /sale/compatibility-list-suggestions
compatibility.products(CompatibleProductsFilter)  // GET /sale/compatible-products
compatibility.productGroups(GroupsFilter)         // GET /sale/compatible-products/groups
```

## G — `client.pricing()`

```java
Pricing pricing = client.pricing();
pricing.feePreview(OfferFeePreviewRequest)        // POST /pricing/offer-fee-preview
pricing.streamCurrentQuotes(QuoteFilter)          // GET  /pricing/offer-quotes       Stream<OfferQuote>
pricing.depositTypes()                            // GET  /deposit/types

PricingAutomation automation = pricing.automation();
automation.streamRules()                          // GET    /sale/price-automation/rules  Stream<PricingRule>
automation.get(ruleId)                            // GET    /sale/price-automation/rules/{ruleId}
automation.create(PricingRuleRequest)             // POST   /sale/price-automation/rules
automation.update(ruleId, PricingRuleRequest)     // PUT    /sale/price-automation/rules/{ruleId}
automation.delete(ruleId)                         // DELETE /sale/price-automation/rules/{ruleId}
automation.rulesOfOffer(offerId)                  // GET    /sale/price-automation/offers/{offerId}/rules

Promotions promotions = pricing.promotions();     // loyalty promotions (multibuy, rebates)
promotions.streamPromotions()                     // GET    /sale/loyalty/promotions  Stream<Promotion>
promotions.get(promotionId)                       // GET    /sale/loyalty/promotions/{promotionId}
promotions.create(PromotionRequest)               // POST   /sale/loyalty/promotions
promotions.modify(promotionId, PromotionRequest)  // PUT    /sale/loyalty/promotions/{promotionId}
promotions.deactivate(promotionId)                // DELETE /sale/loyalty/promotions/{promotionId}

TurnoverDiscounts turnover = pricing.turnoverDiscounts();
turnover.list()                                   // GET /sale/turnover-discount
turnover.set(marketplaceId, TurnoverDiscountRequest) // PUT /sale/turnover-discount/{marketplaceId}
turnover.deactivate(marketplaceId)                // PUT /sale/turnover-discount/{marketplaceId}/deactivate
```

## H — `client.campaigns()`

```java
Campaigns campaigns = client.campaigns();

Badges badges = campaigns.badges();
badges.availableCampaigns()                       // GET   /sale/badge-campaigns
badges.apply(BadgeApplicationRequest)             // POST  /sale/badges                       (async: returns created REQUESTED application; verification is e-mail-notified, not polled)
badges.streamApplications(ApplicationFilter)      // GET   /sale/badge-applications  Stream<BadgeApplication>
badges.application(applicationId)                 // GET   /sale/badge-applications/{applicationId}
badges.streamBadges(BadgeFilter)                  // GET   /sale/badges              Stream<Badge>
badges.update(offerId, campaignId, BadgePatch)    // PATCH /sale/badges/offers/{offerId}/campaigns/{campaignId} (sync)

AllegroPrices allegroPrices = campaigns.allegroPrices();
allegroPrices.participation()                     // GET   /sale/allegro-prices/accounts/participations
allegroPrices.updateParticipation(consent)        // PATCH /sale/allegro-prices/accounts/participations
allegroPrices.offersStatus(offerIds...)           // POST  /sale/allegro-prices/offers-queries
allegroPrices.submitOffers(offerIds...)           // POST  /sale/allegro-prices/offers/submit-offer-commands (sync)
allegroPrices.excludeOffers(offerIds...)          // POST  /sale/allegro-prices/offers/exclusion-commands    (sync)

AlleDiscount alleDiscount = campaigns.alleDiscount();
alleDiscount.streamCampaigns()                    // GET   /sale/alle-discount/campaigns     Stream<…>
alleDiscount.eligibleOffers(campaignId)           // GET   /sale/alle-discount/{campaignId}/eligible-offers
alleDiscount.submittedOffers(campaignId)          // GET   /sale/alle-discount/{campaignId}/submitted-offers
alleDiscount.submitOffer(campaignId, offerId, …)  // POST  /sale/alle-discount/submit-offer-commands   (sync)
alleDiscount.withdrawOffer(campaignId, offerId)   // POST  /sale/alle-discount/withdraw-offer-commands (sync)
```

## I — `client.fulfillment()`

```java
Fulfillment fulfillment = client.fulfillment();

AdvanceShipNotices asn = fulfillment.advanceShipNotices();
asn.streamNotices(AsnFilter)                      // GET    /fulfillment/advance-ship-notices  Stream<Asn>
asn.get(asnId)                                    // GET    /fulfillment/advance-ship-notices/{id}  (ETag -> version())
asn.create(AsnRequest)                            // POST   /fulfillment/advance-ship-notices
asn.update(asnId, AsnRequest, version)            // PUT    /fulfillment/advance-ship-notices/{id}            (If-Match)
asn.updateSubmitted(asnId, Update, version)       // PUT    /fulfillment/advance-ship-notices/{id}/submitted  (If-Match)
asn.submit(asnId) -> SubmitStatus                 // PUT+GET /fulfillment/submit-commands/{command-id} (sync poll)
asn.cancel(asnId)                                 // PUT    /fulfillment/advance-ship-notices/{id}/cancel
asn.delete(asnId)                                 // DELETE /fulfillment/advance-ship-notices/{id}
asn.labels(asnId) -> byte[]                       // GET    /fulfillment/advance-ship-notices/{id}/labels  (PDF)
asn.receivingState(asnId)                         // GET    /fulfillment/advance-ship-notices/{id}/receiving-state
// update/updateSubmitted take the version() from a prior get()/create()/write as the If-Match token
// (the spec requires If-Match on both). ASN `shipping` (polymorphic COURIER_BY_SELLER / OWN_TRANSPORT /
// THIRD_PARTY_DELIVERY) is a deferred follow-up — not on the read model or the write builders yet.

fulfillment.stock()                               // GET /fulfillment/stock
fulfillment.parcelsOf(orderId)                    // GET /fulfillment/orders/{orderId}/parcels
fulfillment.availableProducts()                   // GET /fulfillment/available-products
fulfillment.refundDispositions(Filter)            // GET /fulfillment/returns/refund-dispositions

fulfillment.removalPreference()                   // GET /fulfillment/removal/preferences
fulfillment.setRemovalPreference(Preference)      // PUT /fulfillment/removal/preferences
fulfillment.taxId()                               // GET /fulfillment/tax-id
fulfillment.addTaxId(String taxId)                // POST /fulfillment/tax-id (body is a single `taxId` field)
fulfillment.updateTaxId(String taxId)             // PUT /fulfillment/tax-id (body is a single `taxId` field)
```

## J — `client.messaging()`, `client.disputes()`, `client.contacts()`

```java
Messaging messaging = client.messaging();         // works with seller AND buyer user-tokens
messaging.streamThreads()                         // GET    /messaging/threads       Stream<MessageThread>
messaging.thread(threadId)                        // GET    /messaging/threads/{threadId}
messaging.markRead(threadId)                      // PUT    /messaging/threads/{threadId}/read
messaging.streamMessages(threadId)                // GET    /messaging/threads/{threadId}/messages Stream<Message>
messaging.send(NewMessageRequest)                 // POST   /messaging/messages       (new thread, to user+offer/order)
messaging.reply(threadId, MessageRequest)         // POST   /messaging/threads/{threadId}/messages
messaging.message(messageId)                      // GET    /messaging/messages/{messageId}
messaging.deleteMessage(messageId)                // DELETE /messaging/messages/{messageId}
messaging.declareAttachment(AttachmentDeclaration)// POST   /messaging/message-attachments
messaging.uploadAttachment(attachmentId, bytes)   // PUT    /messaging/message-attachments/{attachmentId}
messaging.downloadAttachment(attachmentId)        // GET    /messaging/message-attachments/{attachmentId}

Disputes disputes = client.disputes();            // post-purchase issues (claims)
disputes.streamIssues(IssueFilter)                // GET  /sale/issues                Stream<IssueSummary>
disputes.get(issueId)                             // GET  /sale/issues/{issueId}
disputes.chat(issueId)                            // GET  /sale/issues/{issueId}/chat
disputes.addMessage(issueId, IssueMessageRequest) // POST /sale/issues/{issueId}/message
disputes.changeStatus(issueId, StatusChange)      // POST /sale/issues/{issueId}/status
disputes.declareAttachment(AttachmentDeclaration) // POST /sale/issues/attachments
disputes.uploadAttachment(attachmentId, bytes)    // PUT  /sale/issues/attachments/{attachmentId}
disputes.downloadAttachment(attachmentId)         // GET  /sale/issues/attachments/{attachmentId}

Contacts contacts = client.contacts();            // seller contact cards for offers
contacts.list()                                   // GET  /sale/offer-contacts
contacts.get(contactId)                           // GET  /sale/offer-contacts/{id}
contacts.create(ContactRequest)                   // POST /sale/offer-contacts
contacts.update(contactId, ContactRequest)        // PUT  /sale/offer-contacts/{id}
```

## K — `client.settings()`

```java
SaleSettings settings = client.settings();

// After-sale service conditions (warranty / return policy / implied warranty)
AfterSaleConditions afterSale = settings.afterSale();
afterSale.streamReturnPolicies() / .returnPolicy(id) // GET …/return-policies[/{id}] (lazy Stream of full policies)
afterSale.createReturnPolicy(ReturnPolicyRequest) // POST /after-sales-service-conditions/return-policies
afterSale.updateReturnPolicy(id, Request)         // PUT  /after-sales-service-conditions/return-policies/{id}
afterSale.deleteReturnPolicy(id)                  // DELETE /after-sales-service-conditions/return-policies/{id}
afterSale.streamWarranties() / .warranty(id)      // GET  …/warranties[/{warrantyId}] (lazy Stream, matches stream* convention)
afterSale.createWarranty(WarrantyRequest)         // POST …/warranties
afterSale.updateWarranty(id, WarrantyRequest)     // PUT  …/warranties/{warrantyId}
afterSale.streamImpliedWarranties() / .impliedWarranty(id) // GET …/implied-warranties[/{id}] (lazy Stream, matches stream* convention)
afterSale.createImpliedWarranty(Request)          // POST …/implied-warranties
afterSale.updateImpliedWarranty(id, Request)      // PUT  …/implied-warranties/{impliedWarrantyId}
afterSale.declareAttachment(AttachmentMetadata)   // POST …/attachments
afterSale.uploadAttachment(attachmentId, bytes)   // PUT  …/attachments/{attachmentId}

// Additional services
AdditionalServices additional = settings.additionalServices();
additional.categoryDefinitions()                  // GET   /sale/offer-additional-services/categories
additional.groups() / .group(groupId)             // GET   /sale/offer-additional-services/groups[/{groupId}]
additional.createGroup(ServiceGroupRequest)       // POST  /sale/offer-additional-services/groups
additional.updateGroup(groupId, ServiceGroupRequest) // PUT /sale/offer-additional-services/groups/{groupId}
additional.translations(groupId)                  // GET   …/groups/{groupId}/translations
additional.setTranslation(groupId, language, Translation) // PATCH …/translations/{language}
additional.deleteTranslation(groupId, language)   // DELETE …/translations/{language}

// Product compliance (GPSR)
Compliance compliance = settings.compliance();
compliance.streamResponsiblePersons()             // GET  /sale/responsible-persons (lazy Stream)
compliance.createResponsiblePerson(Request)       // POST /sale/responsible-persons
compliance.updateResponsiblePerson(id, Request)   // PUT  /sale/responsible-persons/{id}
compliance.streamResponsibleProducers() / .responsibleProducer(id) // GET /sale/responsible-producers[/{id}]
compliance.createResponsibleProducer(Request)     // POST /sale/responsible-producers
compliance.updateResponsibleProducer(id, Request) // PUT  /sale/responsible-producers/{id}

// Size tables
SizeTables sizeTables = settings.sizeTables();
sizeTables.list() / .get(tableId)                 // GET  /sale/size-tables[/{tableId}]
sizeTables.templates()                            // GET  /sale/size-tables-templates
sizeTables.create(SizeTableRequest)               // POST /sale/size-tables
sizeTables.update(tableId, SizeTableRequest)      // PUT  /sale/size-tables/{tableId}

settings.taxSettings(categoryId)                  // GET  /sale/tax-settings
```

---

## Design notes

1. **Coverage:** all 267 spec operations are represented. Command-status endpoints
   (`…-commands/{id}`, `…/tasks`, `operations/{id}`) are consumed INTERNALLY by the *(sync)*
   wrappers and add no public methods — that alone removes ~25 endpoint-shaped methods a 1:1
   mapping would have inflicted on consumers.
2. **Naming rules applied:** verbs from the consumer's intent (`markStatus`, `requestPickup`,
   `assignPackages`), `stream…` = lazy paginated `Stream<T>`, `get` = by id, no `list…All()`.
   Facade root holds the everyday operations; sub-facades group coherent tool-sets the way
   ksef does (`invoices().archive()` ≈ `offers().batch()`).
3. **Cross-bucket append point (new rule):** bucket F appends its accessor lines
   (`tags()`, `translations()`, `bundles()`, `flexibleBundles()`, `rating()`) to the `Offers`
   root interface owned by bucket A — same append-only regime as the three existing append
   points. Registered in the collision rules.
4. **Buyer-token facades:** `bidding()` and `messaging()` work with a buyer user-token — the
   same SDK serves both sides of the marketplace.
5. Method names are the PROPOSED baseline: a bucket owner may rename within intent-naming
   rules during the starter slice, with `/review` as the arbiter; renames after a bucket's
   final PR are breaking changes and need the operator.
