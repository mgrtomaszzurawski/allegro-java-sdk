/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder;

/**
 * Immutable declaration of a file to attach to an issue message, uploaded before
 * it is referenced. The Allegro contract requires the file name and the exact
 * positive byte size.
 *
 * <pre>{@code
 * byte[] bytes = Files.readAllBytes(path);
 * IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
 *         .filename("evidence.jpg")
 *         .size(bytes.length)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class IssueAttachmentDeclaration {

    /** Smallest byte size Allegro accepts for a declaration. */
    public static final int MIN_SIZE_BYTES = 1;

    /** Largest byte size Allegro accepts for an issue attachment (2 MiB). */
    public static final int MAX_SIZE_BYTES = 2_097_152;

    private static final String ERR_FILENAME_REQUIRED = "filename is required";
    private static final String ERR_SIZE_REQUIRED = "size is required";
    private static final String ERR_SIZE_TOO_SMALL =
            "size must be at least " + MIN_SIZE_BYTES + " byte";
    private static final String ERR_SIZE_TOO_LARGE =
            "size must be at most " + MAX_SIZE_BYTES + " bytes";

    private final String filename;
    private final int size;

    private IssueAttachmentDeclaration(Builder builder) {
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
    public int size() {
        return size;
    }

    /** A builder pre-filled from this declaration. */
    public Builder toBuilder() {
        return new Builder().filename(filename).size(size);
    }

    /** Fluent, fail-fast builder for {@link IssueAttachmentDeclaration}. */
    public static final class Builder {

        private String filename;
        private int size;
        private boolean sizeSet;

        private Builder() {
        }

        /** The file name (required). */
        public Builder filename(String name) {
            this.filename = name;
            return this;
        }

        /** The exact byte size (required, {@value #MIN_SIZE_BYTES}..{@value #MAX_SIZE_BYTES}). */
        public Builder size(int sizeInBytes) {
            this.size = sizeInBytes;
            this.sizeSet = true;
            return this;
        }

        /** Validate the required fields and build. */
        public IssueAttachmentDeclaration build() {
            if (filename == null || filename.isBlank()) {
                throw new IllegalStateException(ERR_FILENAME_REQUIRED);
            }
            if (!sizeSet) {
                throw new IllegalStateException(ERR_SIZE_REQUIRED);
            }
            if (size < MIN_SIZE_BYTES) {
                throw new IllegalStateException(ERR_SIZE_TOO_SMALL);
            }
            if (size > MAX_SIZE_BYTES) {
                throw new IllegalStateException(ERR_SIZE_TOO_LARGE);
            }
            return new IssueAttachmentDeclaration(this);
        }
    }
}
