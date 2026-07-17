/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RemovalRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RemovalRequestRaw;
import org.jspecify.annotations.Nullable;

/**
 * The removal state of a received rating, as returned by
 * {@code UserRatings.requestRemoval(...)} and carried on {@link UserRating}:
 * until when a removal may be requested, and the submitted request if any.
 *
 * @param possibleTo latest date a removal request may be submitted (ISO-8601),
 *     or {@code null}
 * @param request the submitted removal request, or {@code null} if none was made
 *
 * @since 0.2.0
 */
public record Removal(@Nullable String possibleTo, @Nullable RemovalRequest request) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Removal from(RemovalRaw raw) {
        RemovalRequestRaw request = raw.getRequest();
        return new Removal(raw.getPossibleTo(),
                request == null ? null : RemovalRequest.from(request));
    }

    /**
     * A submitted request to remove a rating.
     *
     * @param createdAt when the request was created (ISO-8601)
     * @param message the explanation given for the removal
     * @param source who requested the removal, or {@code null} if not reported
     */
    public record RemovalRequest(
            String createdAt,
            String message,
            @Nullable Source source) {

        static RemovalRequest from(RemovalRequestRaw raw) {
            return new RemovalRequest(raw.getCreatedAt(), raw.getMessage(), Source.from(raw.getSource()));
        }
    }

    /** Who requested the removal of a rating. */
    public enum Source {
        SELLER,
        ADMIN;

        static @Nullable Source from(RemovalRequestRaw.@Nullable SourceEnum raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw) {
                case SELLER -> SELLER;
                case ADMIN -> ADMIN;
            };
        }
    }
}
