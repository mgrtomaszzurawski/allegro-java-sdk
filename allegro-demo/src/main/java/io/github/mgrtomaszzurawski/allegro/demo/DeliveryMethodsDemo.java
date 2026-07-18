/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryMethod;
import java.util.List;

/**
 * Bucket C live probe (TESTING.md §2) for {@code shipping().deliveryMethods()}.
 * Delivery methods are read-only reference data offered by Allegro, so this
 * applies the read-only carve-out: instead of write→read it verifies response
 * SHAPE against the live wire — confirm the mapped fields (id, name,
 * {@code paymentPolicy}, flag, countries) actually arrive and parse.
 *
 * <p>The endpoint needs no OAuth scope, so an app-only client-credentials token
 * suffices; this scenario ignores the stored user token and the {@code account}
 * argument — it is therefore runnable even while the seller user token is being
 * re-bootstrapped.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=delivery-methods
 * </pre>
 */
public final class DeliveryMethodsDemo {

    /** Scenario id under which {@link DemoApp} registers this probe. */
    static final String SCENARIO = "delivery-methods";

    private DeliveryMethodsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) {
        try (AllegroClient client = AllegroClient.create(
                new ClientCredentials(clientId, clientSecret), AllegroEnvironment.SANDBOX)) {

            List<DeliveryMethod> methods = client.shipping().deliveryMethods();
            System.out.println("deliveryMethods(): " + methods.size() + " methods");
            if (methods.isEmpty()) {
                System.out.println("(empty - cannot confirm field shape on this account)");
                return;
            }
            DeliveryMethod first = methods.get(0);
            System.out.println("first: id='" + first.id() + "', name='" + first.name()
                    + "', paymentPolicy=" + first.paymentPolicy()
                    + ", allegroEndorsed=" + first.allegroEndorsed()
                    + ", dispatch=" + first.dispatchCountry()
                    + ", destination=" + first.destinationCountry()
                    + ", marketplaces=" + first.marketplaces().size());
        }
    }
}
