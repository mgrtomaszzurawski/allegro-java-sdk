/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder;

/**
 * Immutable declaration of an attachment to be uploaded before it is referenced
 * from a message. The Allegro contract requires the file name and the exact byte
 * size (which must not exceed the server limit).
 *
 * <pre>{@code
 * byte[] bytes = Files.readAllBytes(path);
 * AttachmentDeclaration declaration = AttachmentDeclaration.builder()
 *         .filename("invoice.pdf")
 *         .size(bytes.length)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class AttachmentDeclaration {

    /** Server limit on an attachment's size, in bytes (5 MiB). */
    public static final long MAX_SIZE_BYTES = 5_242_880L;

    private static final long MIN_SIZE_BYTES = 0L;

    private static final String ERR_FILENAME_REQUIRED = "filename is required";
    private static final String ERR_SIZE_REQUIRED = "size is required";
    private static final String ERR_SIZE_NEGATIVE = "size must not be negative";
    private static final String ERR_SIZE_TOO_LARGE =
            "size must be at most " + MAX_SIZE_BYTES + " bytes";

    private final String filename;
    private final long size;

    private AttachmentDeclaration(Builder builder) {
        this.filename = builder.filename;
        this.size = builder.size;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The declared file name. */
    public String filename() {
        return filename;
    }

    /** The declared byte size. */
    public long size() {
        return size;
    }

    /** A builder pre-filled from this declaration. */
    public Builder toBuilder() {
        return new Builder().filename(filename).size(size);
    }

    /** Fluent, fail-fast builder for {@link AttachmentDeclaration}. */
    public static final class Builder {

        private String filename;
        private long size;
        private boolean sizeSet;

        private Builder() {
        }

        /** The file name (required). */
        public Builder filename(String name) {
            this.filename = name;
            return this;
        }

        /** The exact byte size (required, {@code 0..}{@value #MAX_SIZE_BYTES}). */
        public Builder size(long sizeInBytes) {
            this.size = sizeInBytes;
            this.sizeSet = true;
            return this;
        }

        /** Validate the required fields and build. */
        public AttachmentDeclaration build() {
            if (filename == null || filename.isBlank()) {
                throw new IllegalStateException(ERR_FILENAME_REQUIRED);
            }
            if (!sizeSet) {
                throw new IllegalStateException(ERR_SIZE_REQUIRED);
            }
            if (size < MIN_SIZE_BYTES) {
                throw new IllegalStateException(ERR_SIZE_NEGATIVE);
            }
            if (size > MAX_SIZE_BYTES) {
                throw new IllegalStateException(ERR_SIZE_TOO_LARGE);
            }
            return new AttachmentDeclaration(this);
        }
    }
}
