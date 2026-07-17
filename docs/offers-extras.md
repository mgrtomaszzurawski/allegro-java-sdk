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
    for (ClassifiedPackage pkg : client.classifieds().availablePackages(categoryId)) {
        System.out.println(pkg.name() + " (" + pkg.type() + ")");
        pkg.promotions().forEach(promo ->
                System.out.println("  promotion " + promo.name() + " for " + promo.duration()));
    }
}
```

Each `ClassifiedPackage` exposes its `id`, `name`, `type`
(`BASE` or `EXTRA`), the bundled `extensions` and `promotions`, and the
`publication` terms (each duration is a `java.time.Duration`). Package
configurations are public reference data, so an app-only client-credentials
grant is sufficient — no user context is required.
