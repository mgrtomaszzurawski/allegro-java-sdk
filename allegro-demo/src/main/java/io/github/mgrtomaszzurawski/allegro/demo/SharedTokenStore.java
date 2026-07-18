/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flock-guarded refresh-token store shared by every agent (ADR-008).
 *
 * <p>Allegro ROTATES refresh tokens on every refresh, so concurrent refreshes
 * from independent processes would invalidate each other's sessions. Two
 * safeguards keep the shared file correct:
 *
 * <ul>
 *   <li><b>Mutual exclusion</b> — every read and write is serialized by an
 *       exclusive OS lock held on a SEPARATE, stable lock file
 *       ({@code <store>.lock}). Every agent container shares one kernel and the
 *       same {@code ext4} volume, so this fcntl lock serializes writers across
 *       containers. The lock lives on its own file (never renamed) so the
 *       guarantee survives the atomic replace of the data file below.</li>
 *   <li><b>Crash-atomic writes</b> — a write serializes the merged entries to a
 *       sibling temp file and then {@code ATOMIC_MOVE}s it over the data file.
 *       A process killed mid-write (e.g. a demo hitting its timeout while
 *       refreshing) leaves either the untouched old file or the complete new
 *       one — never a truncated file that drops the other account's token. The
 *       previous in-place {@code setLength(0)}+write could empty the file if the
 *       process died in that window, which is how a shared token was lost.</li>
 * </ul>
 *
 * <p>Format: one {@code account=token} line per account (seller/buyer).
 * Location: {@code ALLEGRO_TOKEN_STORE} env var, defaulting to
 * {@code /workspace/shared/secrets/allegro-sandbox-tokens.properties}.
 *
 * <p>Agent infrastructure — not part of the published SDK; applications
 * persist {@code AllegroClient.refreshToken()} however they like.
 */
final class SharedTokenStore {

    private static final String STORE_ENV_VAR = "ALLEGRO_TOKEN_STORE";
    private static final String DEFAULT_STORE_PATH =
            "/workspace/shared/secrets/allegro-sandbox-tokens.properties";
    private static final String LOCK_SUFFIX = ".lock";
    private static final String TEMP_PREFIX = ".tokens-";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String READ_WRITE_MODE = "rw";
    private static final Pattern ENTRY_LINE = Pattern.compile("^([a-z]+)=(\\S+)$");
    private static final String ENTRY_FORMAT = "%s=%s%n";
    private static final Set<PosixFilePermission> OWNER_ONLY =
            Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private final Path storePath;
    private final Path lockPath;

    SharedTokenStore() {
        this(defaultStorePath());
    }

    /** Test seam: point the store at an arbitrary path. */
    SharedTokenStore(Path storePath) {
        this.storePath = storePath.toAbsolutePath();
        this.lockPath = this.storePath.resolveSibling(this.storePath.getFileName() + LOCK_SUFFIX);
    }

    private static Path defaultStorePath() {
        String configured = System.getenv(STORE_ENV_VAR);
        return Path.of(configured != null ? configured : DEFAULT_STORE_PATH);
    }

    /** Stored refresh token for the account, or {@code null}. */
    String load(String accountKey) throws IOException {
        try (RandomAccessFile lockFile = new RandomAccessFile(lockPath.toFile(), READ_WRITE_MODE)) {
            restrictPermissions(lockPath);
            try (FileLock ignored = lockFile.getChannel().lock()) {
                return parse(readStore()).get(accountKey);
            }
        }
    }

    /** Atomically upsert the account's refresh token under the exclusive lock. */
    void store(String accountKey, String refreshToken) throws IOException {
        try (RandomAccessFile lockFile = new RandomAccessFile(lockPath.toFile(), READ_WRITE_MODE)) {
            restrictPermissions(lockPath);
            try (FileLock ignored = lockFile.getChannel().lock()) {
                Map<String, String> entries = parse(readStore());
                entries.put(accountKey, refreshToken);
                writeAtomically(entries);
            }
        }
    }

    /** Read the data file's raw contents, or empty when it does not exist yet. */
    private String readStore() throws IOException {
        if (!Files.exists(storePath)) {
            return "";
        }
        return Files.readString(storePath, StandardCharsets.UTF_8);
    }

    /**
     * Serialize all entries to a sibling temp file, then atomically rename it
     * over the data file so a mid-write crash can never leave a partial file.
     */
    private void writeAtomically(Map<String, String> entries) throws IOException {
        StringBuilder content = new StringBuilder();
        entries.forEach((account, token) -> content.append(ENTRY_FORMAT.formatted(account, token)));
        Path temp = Files.createTempFile(storePath.getParent(), TEMP_PREFIX, TEMP_SUFFIX);
        try {
            restrictPermissions(temp);
            Files.writeString(temp, content.toString(), StandardCharsets.UTF_8);
            moveIntoPlace(temp);
        } finally {
            // No-op after a successful ATOMIC_MOVE (temp no longer exists);
            // fires only when the write or move failed, leaving the temp behind.
            Files.deleteIfExists(temp);
        }
        // The atomic path inherits the temp's 0600; this guards the non-atomic
        // fallback in moveIntoPlace, where a copy need not preserve permissions.
        restrictPermissions(storePath);
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, storePath,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, storePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Owner-only permissions — tokens at rest. */
    private static void restrictPermissions(Path path) throws IOException {
        Files.setPosixFilePermissions(path, OWNER_ONLY);
    }

    private static Map<String, String> parse(String content) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : content.split("\\R")) {
            Matcher matcher = ENTRY_LINE.matcher(line.trim());
            if (matcher.matches()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
        }
        return entries;
    }
}
