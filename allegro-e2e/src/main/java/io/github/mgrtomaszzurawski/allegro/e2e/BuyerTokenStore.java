/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flock-guarded refresh-token store, shared by every agent (ADR-008), addressed
 * from the buyer-side E2E layer so a minted buyer token persists for reuse.
 *
 * <p>This is a deliberate, file-format-compatible parallel of
 * {@code allegro-demo}'s {@code SharedTokenStore}: same file, same
 * {@code account=token} lines, same on-locked-channel flock discipline, same
 * {@code 0600} permissions. It is duplicated rather than shared because that
 * class is package-private agent infrastructure in another (non-published)
 * module. Every write preserves the other accounts' entries, so persisting the
 * {@code buyer} token never disturbs the {@code seller} token. Consolidating the
 * two into one small agent-infra utility is tracked as a follow-up.
 *
 * <p>Location: the {@code ALLEGRO_TOKEN_STORE} env var, defaulting to
 * {@code /workspace/shared/secrets/allegro-sandbox-tokens.properties}.
 */
final class BuyerTokenStore {

    static final String BUYER_ACCOUNT = "buyer";

    private static final String STORE_ENV_VAR = "ALLEGRO_TOKEN_STORE";
    private static final String DEFAULT_STORE_PATH =
            "/workspace/shared/secrets/allegro-sandbox-tokens.properties";
    private static final String READ_WRITE_MODE = "rw";
    private static final Pattern ENTRY_LINE = Pattern.compile("^([a-z]+)=(\\S+)$");
    private static final String ENTRY_FORMAT = "%s=%s%n";

    private final Path storePath;

    BuyerTokenStore() {
        String configured = System.getenv(STORE_ENV_VAR);
        this.storePath = Path.of(configured != null ? configured : DEFAULT_STORE_PATH);
    }

    /** Stored refresh token for the account, or {@code null} when absent. */
    String load(String accountKey) {
        try (RandomAccessFile file = openSecured();
                FileLock ignored = file.getChannel().lock()) {
            // All I/O goes through the LOCKED channel: opening a second channel
            // (e.g. Files.readString) and closing it would drop every advisory
            // lock this JVM holds on the file (POSIX semantics).
            return parse(readAll(file)).get(accountKey);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read the shared token store", failure);
        }
    }

    /** Atomically upsert the account's refresh token under the exclusive lock. */
    void store(String accountKey, String refreshToken) {
        try (RandomAccessFile file = openSecured();
                FileLock ignored = file.getChannel().lock()) {
            Map<String, String> entries = parse(readAll(file));
            entries.put(accountKey, refreshToken);
            StringBuilder content = new StringBuilder();
            entries.forEach((account, token) -> content.append(ENTRY_FORMAT.formatted(account, token)));
            byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
            file.setLength(0);
            file.seek(0);
            file.write(bytes);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to write the shared token store", failure);
        }
    }

    /** Open (creating if absent) with owner-only permissions — tokens at rest. */
    private RandomAccessFile openSecured() throws IOException {
        RandomAccessFile file = new RandomAccessFile(storePath.toFile(), READ_WRITE_MODE);
        try {
            Files.setPosixFilePermissions(storePath,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (IOException failure) {
            file.close();
            throw failure;
        }
        return file;
    }

    private static String readAll(RandomAccessFile file) throws IOException {
        file.seek(0);
        byte[] bytes = new byte[(int) file.length()];
        file.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
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
