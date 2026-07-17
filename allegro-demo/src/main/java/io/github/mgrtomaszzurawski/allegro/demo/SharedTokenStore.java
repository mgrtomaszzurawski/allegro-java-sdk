/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flock-guarded refresh-token store shared by every agent (ADR-008).
 *
 * <p>Allegro ROTATES refresh tokens on every refresh, so concurrent refreshes
 * from independent processes would invalidate each other's sessions. This
 * store serializes writes with an exclusive OS-level file lock; readers grab
 * the same lock briefly. Format: one {@code account=token} line per account
 * (seller/buyer). Location: {@code ALLEGRO_TOKEN_STORE} env var, defaulting to
 * {@code /workspace/shared/.allegro-sandbox-tokens.properties}.
 *
 * <p>Agent infrastructure — not part of the published SDK; applications
 * persist {@code AllegroClient.refreshToken()} however they like.
 */
final class SharedTokenStore {

    private static final String STORE_ENV_VAR = "ALLEGRO_TOKEN_STORE";
    private static final String DEFAULT_STORE_PATH =
            "/workspace/shared/.allegro-sandbox-tokens.properties";
    private static final String READ_WRITE_MODE = "rw";
    private static final Pattern ENTRY_LINE = Pattern.compile("^([a-z]+)=(\\S+)$");
    private static final String ENTRY_FORMAT = "%s=%s%n";

    private final Path storePath;

    SharedTokenStore() {
        String configured = System.getenv(STORE_ENV_VAR);
        this.storePath = Path.of(configured != null ? configured : DEFAULT_STORE_PATH);
    }

    /** Stored refresh token for the account, or {@code null}. */
    String load(String accountKey) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(storePath.toFile(), READ_WRITE_MODE);
                FileLock ignored = file.getChannel().lock()) {
            return parse(Files.readString(storePath)).get(accountKey);
        }
    }

    /** Atomically upsert the account's refresh token under the exclusive lock. */
    void store(String accountKey, String refreshToken) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(storePath.toFile(), READ_WRITE_MODE);
                FileLock ignored = file.getChannel().lock()) {
            Map<String, String> entries = parse(Files.readString(storePath));
            entries.put(accountKey, refreshToken);
            StringBuilder content = new StringBuilder();
            entries.forEach((account, token) -> content.append(ENTRY_FORMAT.formatted(account, token)));
            Files.writeString(storePath, content.toString());
        }
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
