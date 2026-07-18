# Offers extras & classifieds (bucket F)

Consumer usage guide for the offer add-ons — classifieds (advertisement)
packages and statistics, offer tags, translations, rating, and bundles.

This bucket is landing incrementally. Sections appear as each area ships; the
authoritative method layout is [`API-SURFACE.md`](../API-SURFACE.md) §F.

## Classifieds

Classifieds are Allegro's advertisement listings (for example automotive or
real-estate categories). Reach them via `client.classifieds()`.

### Available packages in a category

The packages a seller can attach to an advertisement depend on the category the
advertisement is listed in. Read them with `availablePackages(categoryId)`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedPackage;

try (AllegroClient client = AllegroClient.create(credentials, environment)) {
    for (ClassifiedPackage classifiedPackage : client.classifieds().availablePackages(categoryId)) {
        System.out.println(classifiedPackage.name() + " (" + classifiedPackage.type() + ")");
        classifiedPackage.promotions().forEach(promo ->
                System.out.println("  promotion " + promo.name() + " for " + promo.duration()));
    }
}
```

Each `ClassifiedPackage` exposes its `id`, `name`, `type`
(`BASE` or `EXTRA`), the bundled `extensions` and `promotions`, and the
`publication` terms (each duration is a `java.time.Duration`). The endpoint
requires a **user (seller) access token** with the `sale:offers:read` scope
(the spec declares it `bearer-token-for-user`), so authenticate with an
authorization-code or device grant — an app-only client-credentials token is
not sufficient.

Read one package configuration directly by id with `getPackage(packageId)`.

### Assigning packages to an offer

An advertisement offer carries exactly one **base** package and any number of
**extra** packages. Read the current assignment with `packagesOfOffer(offerId)`
and replace it with `assignPackages(offerId, assignment)`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.ClassifiedAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model.OfferClassifieds;

ClassifiedAssignment assignment = ClassifiedAssignment.builder()
        .basePackage(basePackageId)
        .addExtraPackage(extraPackageId, true)   // true republishes the advert
        .build();
client.classifieds().assignPackages(offerId, assignment);

OfferClassifieds assigned = client.classifieds().packagesOfOffer(offerId);
System.out.println("base package: " + assigned.basePackageId());
```

The builder validates that a base package is set (fail-fast); extra packages are
optional and each may set a `republish` flag. `assignPackages` writes with the
`sale:offers:write` scope, so it needs a **user (seller) access token** just like
the reads.
