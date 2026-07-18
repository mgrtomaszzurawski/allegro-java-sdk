/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import java.util.List;

/**
 * Bulk offer operations — reached via {@code offers().batch()}. Each method
 * submits an Allegro batch command, waits for it to finish, and returns the
 * terminal {@link BatchReport}; the asynchronous command/poll/task-paging
 * mechanics are fully internal (no {@code CompletableFuture} in the surface).
 *
 * @since 0.2.0
 */
public interface OfferBatch {

    /**
     * Publish (activate) the given offers in one command.
     *
     * @param offerIds the offers to publish
     * @return the command report once every offer has been processed
     */
    BatchReport publish(List<String> offerIds);

    /**
     * Unpublish (end) the given offers in one command.
     *
     * @param offerIds the offers to unpublish
     * @return the command report once every offer has been processed
     */
    BatchReport unpublish(List<String> offerIds);
}
