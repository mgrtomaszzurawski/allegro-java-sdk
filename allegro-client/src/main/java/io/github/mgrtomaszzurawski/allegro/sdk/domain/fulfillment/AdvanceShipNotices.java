/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.SubmittedAsnUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AdvanceShipNotice;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReceivingState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.SubmitStatus;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Advance Ship Notices (ASN) — the lifecycle of a declared shipment into a One
 * Fulfillment warehouse, reached via {@code fulfillment().advanceShipNotices()}.
 *
 * <p>A notice starts as a {@code DRAFT} ({@link #create(AsnRequest)}), can be
 * edited ({@link #update}) and {@link #submit(String) submitted}; once submitted
 * it can still be amended ({@link #updateSubmitted}) until the warehouse begins
 * unpacking, whose progress is read via {@link #receivingState(String)}.
 *
 * <p><strong>Optimistic concurrency.</strong> {@link #get(String)} and the create
 * / update writes return an {@link AdvanceShipNotice} whose
 * {@link AdvanceShipNotice#version() version} is the server's current {@code ETag}.
 * {@link #update} and {@link #updateSubmitted} require that version as their
 * {@code If-Match} token: pass the value from the notice you just read, and the
 * write fails rather than clobbering a concurrent change if the notice moved on.
 *
 * <p>Operations require the {@code fulfillment:read} / {@code fulfillment:write}
 * OAuth scopes and an account enrolled in One Fulfillment.
 *
 * @since 0.4.0
 */
public interface AdvanceShipNotices {

    /**
     * Stream every Advance Ship Notice.
     *
     * @return a lazy stream over all notices
     */
    Stream<AdvanceShipNotice> streamNotices();

    /**
     * Stream Advance Ship Notices matching a filter.
     *
     * @param filter the statuses to include (use {@link AsnFilter#all()} for none)
     * @return a lazy stream over the matching notices
     */
    Stream<AdvanceShipNotice> streamNotices(AsnFilter filter);

    /**
     * Read a single Advance Ship Notice, including its current
     * {@link AdvanceShipNotice#version() version}.
     *
     * @param asnId the notice identifier
     * @return the notice
     */
    AdvanceShipNotice get(String asnId);

    /**
     * Create a draft Advance Ship Notice.
     *
     * @param request the notice to create
     * @return the created notice, carrying its version
     */
    AdvanceShipNotice create(AsnRequest request);

    /**
     * Fully update an Advance Ship Notice, guarding the write with the notice's
     * current version.
     *
     * @param asnId   the notice identifier
     * @param request the new notice content
     * @param version the {@code If-Match} version from a prior read or write
     * @return the updated notice, carrying its new version
     */
    AdvanceShipNotice update(String asnId, AsnRequest request, String version);

    /**
     * Amend an already-submitted Advance Ship Notice, guarding the write with the
     * notice's current version.
     *
     * @param asnId   the notice identifier
     * @param update  the fields to amend
     * @param version the {@code If-Match} version from a prior read or write
     * @return the amended notice, carrying its new version
     */
    AdvanceShipNotice updateSubmitted(String asnId, SubmittedAsnUpdate update, String version);

    /**
     * Submit a draft Advance Ship Notice to the warehouse and wait for the submit
     * command to reach a terminal state.
     *
     * @param asnId the notice identifier
     * @return the terminal outcome; {@link SubmitStatus#FAILED} means the notice
     *         was not accepted
     */
    SubmitStatus submit(String asnId);

    /**
     * Submit a draft Advance Ship Notice, waiting at most {@code timeout} for a
     * terminal state.
     *
     * @param asnId   the notice identifier
     * @param timeout the longest to wait before giving up
     * @return the terminal outcome; {@link SubmitStatus#FAILED} means the notice
     *         was not accepted
     */
    SubmitStatus submit(String asnId, Duration timeout);

    /**
     * Cancel an Advance Ship Notice.
     *
     * @param asnId the notice identifier
     */
    void cancel(String asnId);

    /**
     * Delete a draft Advance Ship Notice.
     *
     * @param asnId the notice identifier
     */
    void delete(String asnId);

    /**
     * Download the printable labels for an Advance Ship Notice as a PDF.
     *
     * @param asnId the notice identifier
     * @return the PDF bytes
     */
    byte[] labels(String asnId);

    /**
     * Read the warehouse's receiving progress for a submitted Advance Ship Notice.
     *
     * @param asnId the notice identifier
     * @return the receiving state
     */
    ReceivingState receivingState(String asnId);
}
