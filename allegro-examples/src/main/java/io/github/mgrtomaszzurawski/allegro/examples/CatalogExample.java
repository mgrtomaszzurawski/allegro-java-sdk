/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogCategories;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.CatalogProducts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductSearchRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Category;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.Product;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryParameterType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategorySuggestion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.DictionaryValue;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/catalog.md} snippet — if the documented
 * catalogue API stops compiling, this module breaks the build.
 */
public final class CatalogExample {

    private CatalogExample() {
    }

    static long leafRootCount(String clientId, String clientSecret) {
        // Category reads are public data: an app-only client-credentials token is enough.
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            CatalogCategories categories = client.catalog().categories();

            List<Category> roots = categories.roots();
            Category first = roots.get(0);
            List<Category> children = categories.childrenOf(first.id());
            Category category = categories.get(first.id());

            boolean canCreateProduct = category.options() != null
                    && category.options().productCreationEnabled();
            System.out.println(category.name() + " canCreateProduct=" + canCreateProduct
                    + " children=" + children.size());

            for (CategoryParameter parameter : categories.parameters(first.id())) {
                if (parameter.type() == CategoryParameterType.DICTIONARY
                        && !parameter.dictionary().isEmpty()) {
                    DictionaryValue value = parameter.dictionary().get(0);
                    System.out.println(parameter.name() + " -> " + value.id() + "=" + value.value());
                }
            }
            for (CategorySuggestion match : categories.suggest(category.name())) {
                System.out.println("match " + match.id() + " isRoot=" + (match.parent() == null));
            }

            return roots.stream().filter(Category::leaf).count();
        }
    }

    static long productMatches(String clientId, String clientSecret, String phrase) {
        // Product search needs the sale:offers:read scope (a user-context token).
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            return client.catalog().products()
                    .search(ProductSearchRequest.byPhrase(phrase))
                    .limit(20)
                    .peek(summary -> System.out.println(summary.id() + " " + summary.name()))
                    .count();
        }
    }

    static int firstProductParameters(String clientId, String clientSecret, String phrase) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {
            CatalogProducts products = client.catalog().products();
            ProductSummary firstHit = products.search(ProductSearchRequest.byPhrase(phrase))
                    .findFirst().orElseThrow();
            Product product = products.get(firstHit.id());
            System.out.println(product.name() + " has " + product.parameters().size() + " parameters");
            return product.parameters().size();
        }
    }
}
