// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.sirmax.shared.SirmaxException;

/**
 * Where SIRMAX keeps its data on disk.
 *
 * <p>The database and backups live <strong>outside</strong> the application's binaries so an upgrade
 * never wipes operator data (master prompt §44). On Windows that is {@code
 * %LOCALAPPDATA%\SIRMAX}; elsewhere {@code $XDG_DATA_HOME/SIRMAX} or {@code ~/.local/share/SIRMAX}.
 */
public final class AppPaths {

    private final Path root;

    private AppPaths(Path root) {
        this.root = root;
    }

    public static AppPaths resolveDefault() {
        return new AppPaths(defaultRoot());
    }

    public static AppPaths under(Path root) {
        return new AppPaths(root);
    }

    public Path root() {
        return root;
    }

    public Path dataDir() {
        return ensure(root.resolve("data"));
    }

    public Path backupsDir() {
        return ensure(root.resolve("backups"));
    }

    public Path logsDir() {
        return ensure(root.resolve("logs"));
    }

    public Path databaseFile() {
        return dataDir().resolve("sirmax.sqlite");
    }

    private static Path defaultRoot() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path base = (localAppData != null && !localAppData.isBlank())
                    ? Path.of(localAppData)
                    : Path.of(home, "AppData", "Local");
            return base.resolve("SIRMAX");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        Path base = (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : Path.of(home, ".local", "share");
        return base.resolve("SIRMAX");
    }

    private static Path ensure(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new SirmaxException("Could not create directory " + dir, e);
        }
    }
}
