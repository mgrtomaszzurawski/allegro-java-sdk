/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for the shared refresh-token store. The store lost a live
 * token in production when a demo was killed mid-write; these tests pin the
 * merge-preserves-siblings and crash-atomic-write guarantees. Pure filesystem,
 * no live traffic.
 */
class SharedTokenStoreTest {

    private static final String STORE_FILE_NAME = "allegro-sandbox-tokens.properties";
    private static final String SELLER = "seller";
    private static final String BUYER = "buyer";
    private static final String UNKNOWN_ACCOUNT = "nobody";
    private static final String TOKEN_ONE = "refresh-token-one";
    private static final String TOKEN_TWO = "refresh-token-two";
    private static final String TOKEN_ROTATED = "refresh-token-rotated";
    private static final String TEMP_GLOB = ".tokens-*";
    private static final Set<PosixFilePermission> OWNER_ONLY =
            Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    @TempDir
    private Path directory;

    private SharedTokenStore storeAt() {
        return new SharedTokenStore(directory.resolve(STORE_FILE_NAME));
    }

    @Test
    void store_whenAccountStored_loadReturnsToken() throws IOException {
        // given
        SharedTokenStore store = storeAt();

        // when
        store.store(SELLER, TOKEN_ONE);

        // then
        assertEquals(TOKEN_ONE, store.load(SELLER));
    }

    @Test
    void store_whenSecondAccountStored_preservesFirst() throws IOException {
        // given a seller token already persisted
        SharedTokenStore store = storeAt();
        store.store(SELLER, TOKEN_ONE);

        // when the buyer token is stored, then the seller token is rotated
        store.store(BUYER, TOKEN_TWO);
        store.store(SELLER, TOKEN_ROTATED);

        // then both keys survive — the buyer entry is never dropped (the bug)
        assertEquals(TOKEN_ROTATED, store.load(SELLER));
        assertEquals(TOKEN_TWO, store.load(BUYER));
    }

    @Test
    void store_whenReStoringAccount_replacesValueInPlace() throws IOException {
        // given
        SharedTokenStore store = storeAt();
        store.store(SELLER, TOKEN_ONE);

        // when
        store.store(SELLER, TOKEN_ROTATED);

        // then the file holds a single seller line with the new value
        String contents = Files.readString(directory.resolve(STORE_FILE_NAME));
        assertEquals(1, contents.lines().count());
        assertEquals(TOKEN_ROTATED, store.load(SELLER));
    }

    @Test
    void store_whenWriteCompletes_leavesNoTempFileBehind() throws IOException {
        // given
        SharedTokenStore store = storeAt();

        // when
        store.store(SELLER, TOKEN_ONE);
        store.store(BUYER, TOKEN_TWO);

        // then the atomic-move temp files are cleaned up (no partial artefacts)
        assertFalse(hasTempLeftover(), "atomic-write temp file was left behind");
    }

    @Test
    void store_whenPersisting_dataFileIsOwnerOnly() throws IOException {
        // given
        SharedTokenStore store = storeAt();

        // when
        store.store(SELLER, TOKEN_ONE);

        // then tokens at rest are readable only by the owner
        Set<PosixFilePermission> permissions =
                Files.getPosixFilePermissions(directory.resolve(STORE_FILE_NAME));
        assertEquals(OWNER_ONLY, permissions);
    }

    @Test
    void store_whenRewriting_replacesFileAtomicallyNotInPlace() throws IOException {
        // given a store file already written once
        SharedTokenStore store = storeAt();
        store.store(SELLER, TOKEN_ONE);
        Object inodeBefore = fileKey();

        // when it is rewritten
        store.store(BUYER, TOKEN_TWO);
        Object inodeAfter = fileKey();

        // then the data file was replaced by an atomic rename (new inode), not
        // truncated in place — the in-place path was the crash-loses-token bug
        assertNotEquals(inodeBefore, inodeAfter);
    }

    @Test
    void load_whenStoreMissing_returnsNull() throws IOException {
        // given a store whose file does not exist yet
        SharedTokenStore store = storeAt();

        // when / then
        assertNull(store.load(SELLER));
    }

    @Test
    void load_whenAccountAbsent_returnsNull() throws IOException {
        // given
        SharedTokenStore store = storeAt();
        store.store(SELLER, TOKEN_ONE);

        // when / then
        assertNull(store.load(UNKNOWN_ACCOUNT));
    }

    private Object fileKey() throws IOException {
        return Files.readAttributes(directory.resolve(STORE_FILE_NAME),
                BasicFileAttributes.class).fileKey();
    }

    private boolean hasTempLeftover() throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory, TEMP_GLOB)) {
            return entries.iterator().hasNext();
        }
    }
}
