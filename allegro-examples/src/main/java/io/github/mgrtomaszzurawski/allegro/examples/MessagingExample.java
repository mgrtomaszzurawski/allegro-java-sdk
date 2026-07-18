/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.NewMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.ReplyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.AttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.Message;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageThread;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/messaging.md} snippets — if the documented
 * message-center usage stops compiling, this module breaks the build.
 */
public final class MessagingExample {

    private static final String MIME_PDF = "application/pdf";

    private MessagingExample() {
    }

    static List<MessageThread> recentThreads(AllegroCredentials credentials) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            // Streams are lazy — take only what you consume.
            return client.messaging().streamThreads().limit(20).toList();
        }
    }

    static List<Message> threadMessages(AllegroCredentials credentials, String threadId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            client.messaging().markRead(threadId);
            return client.messaging().streamMessages(threadId).toList();
        }
    }

    static Message answerBuyer(AllegroCredentials credentials, String threadId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            return client.messaging().reply(threadId,
                    ReplyRequest.builder().text("Your parcel ships today.").build());
        }
    }

    static Message openThreadAboutOrder(AllegroCredentials credentials, String buyerLogin,
            String orderId) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            return client.messaging().send(NewMessageRequest.builder()
                    .recipientLogin(buyerLogin)
                    .orderId(orderId)
                    .text("Thanks for your order!")
                    .build());
        }
    }

    static Message replyWithAttachment(AllegroCredentials credentials, String threadId,
            byte[] invoicePdf) {
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            // Declare, upload the bytes, then reference the attachment in the reply.
            AttachmentRef declared = client.messaging().declareAttachment(AttachmentDeclaration.builder()
                    .filename("invoice.pdf")
                    .size(invoicePdf.length)
                    .build());
            client.messaging().uploadAttachment(declared.id(), invoicePdf, MIME_PDF);
            return client.messaging().reply(threadId, ReplyRequest.builder()
                    .text("Invoice attached.")
                    .attachment(declared.id())
                    .build());
        }
    }
}
