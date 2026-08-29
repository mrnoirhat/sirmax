// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.sirmax.application.port.CloudBackupTarget;
import org.sirmax.shared.SirmaxException;

/**
 * Uploads backups to a folder in the municipality's own Google Drive (master prompt §41).
 *
 * <p>Deliberately built on the plain Drive REST API over {@link HttpClient} rather than Google's
 * client library: SIRMAX needs three calls — refresh a token, upload a file, check a file exists —
 * and pulling in a transitive tree of gRPC and Guava to make them would be a poor trade for a
 * desktop application that has to install cleanly on a municipal PC.
 *
 * <p>Credentials belong to the municipality: an OAuth client they registered, and a refresh token
 * their administrator granted through the consent screen. SIRMAX ships no client id of its own, so
 * an installation that has not connected an account simply has no off-site copies — which §41 treats
 * as a supported way to run, not a degraded one.
 *
 * <p>Access tokens are held in memory only, and refreshed when they expire. The refresh token comes
 * from {@link SecretStore}, which on Windows is backed by DPAPI (§43).
 */
public final class GoogleDriveBackupTarget implements CloudBackupTarget {

    private static final String PROVIDER = "GOOGLE_DRIVE";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String UPLOAD_ENDPOINT =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final String FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files/";

    private static final String KEY_CLIENT_ID = "drive.client_id";
    private static final String KEY_CLIENT_SECRET = "drive.client_secret";
    private static final String KEY_REFRESH_TOKEN = "drive.refresh_token";
    private static final String KEY_ACCOUNT = "drive.account";

    /** Refresh a minute early; a token that expires mid-upload fails the whole transfer. */
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60);

    private static final ObjectMapper M = new ObjectMapper();

    private final SecretStore secrets;
    private final HttpClient http;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public GoogleDriveBackupTarget(SecretStore secrets) {
        this(secrets, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    }

    GoogleDriveBackupTarget(SecretStore secrets, HttpClient http) {
        this.secrets = secrets;
        this.http = http;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean isConfigured() {
        return secrets.find(KEY_CLIENT_ID).isPresent()
                && secrets.find(KEY_CLIENT_SECRET).isPresent()
                && secrets.find(KEY_REFRESH_TOKEN).isPresent();
    }

    @Override
    public Optional<String> connectedAccount() {
        return secrets.find(KEY_ACCOUNT);
    }

    @Override
    public String upload(String storagePath, String fileName, String folderId) {
        requireConfigured();
        Path file = Path.of(storagePath);
        if (!Files.isRegularFile(file)) {
            throw new SirmaxException("The backup archive is missing: " + storagePath);
        }

        String boundary = "sirmax-" + java.util.UUID.randomUUID();
        byte[] body;
        try {
            body = multipartBody(boundary, fileName, folderId, Files.readAllBytes(file));
        } catch (IOException e) {
            throw new SirmaxException("Could not read the backup archive to upload", e);
        }

        HttpRequest request =
                HttpRequest.newBuilder(URI.create(UPLOAD_ENDPOINT))
                        .header("Authorization", "Bearer " + accessToken())
                        .header("Content-Type", "multipart/related; boundary=" + boundary)
                        .timeout(Duration.ofMinutes(10))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

        HttpResponse<String> response = send(request, "upload the backup");
        if (response.statusCode() / 100 != 2) {
            throw new SirmaxException(
                    "Google Drive refused the upload (" + response.statusCode() + ")");
        }
        return readJson(response.body()).path("id").asText();
    }

    @Override
    public boolean exists(String fileId) {
        requireConfigured();
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(FILES_ENDPOINT + fileId + "?fields=id,trashed"))
                        .header("Authorization", "Bearer " + accessToken())
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
        HttpResponse<String> response = send(request, "check the backup");
        if (response.statusCode() == 404) {
            return false;
        }
        if (response.statusCode() / 100 != 2) {
            throw new SirmaxException(
                    "Google Drive refused the query (" + response.statusCode() + ")");
        }
        // A trashed file is not a backup any more, even though Drive still answers for it.
        return !readJson(response.body()).path("trashed").asBoolean(false);
    }

    /** Exchange the stored refresh token for an access token, reusing it until it nears expiry. */
    private String accessToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiry.minus(EXPIRY_MARGIN))) {
            return accessToken;
        }
        String form =
                "client_id=" + encode(secrets.require(KEY_CLIENT_ID))
                        + "&client_secret=" + encode(secrets.require(KEY_CLIENT_SECRET))
                        + "&refresh_token=" + encode(secrets.require(KEY_REFRESH_TOKEN))
                        + "&grant_type=refresh_token";

        HttpRequest request =
                HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                        .build();

        HttpResponse<String> response = send(request, "sign in to Google Drive");
        if (response.statusCode() / 100 != 2) {
            throw new SirmaxException(
                    "Google refused the stored credentials (" + response.statusCode()
                            + "). Reconnect the account in Configuración.");
        }
        JsonNode token = readJson(response.body());
        accessToken = token.path("access_token").asText();
        accessTokenExpiry = Instant.now().plusSeconds(token.path("expires_in").asLong(3600));
        return accessToken;
    }

    /** Drive's multipart/related upload: a JSON metadata part, then the bytes. */
    private static byte[] multipartBody(
            String boundary, String fileName, String folderId, byte[] content) {
        var metadata = M.createObjectNode();
        metadata.put("name", fileName);
        metadata.putArray("parents").add(folderId);

        String head =
                "--" + boundary + "\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                        + metadata
                        + "\r\n--" + boundary + "\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n";
        String tail = "\r\n--" + boundary + "--";

        byte[] headBytes = head.getBytes(StandardCharsets.UTF_8);
        byte[] tailBytes = tail.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headBytes.length + content.length + tailBytes.length];
        System.arraycopy(headBytes, 0, body, 0, headBytes.length);
        System.arraycopy(content, 0, body, headBytes.length, content.length);
        System.arraycopy(tailBytes, 0, body, headBytes.length + content.length, tailBytes.length);
        return body;
    }

    private HttpResponse<String> send(HttpRequest request, String what) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // Offline is normal for this application (§1.5), so this reads as a condition to
            // report rather than a fault to escalate.
            throw new SirmaxException("Could not reach Google Drive to " + what, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SirmaxException("Interrupted while trying to " + what, e);
        }
    }

    private static JsonNode readJson(String body) {
        try {
            return M.readTree(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new SirmaxException("Google Drive returned something unexpected", e);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new SirmaxException("No Google account is connected for backups");
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
