// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.sirmax.shared.i18n.MessageKey;

/**
 * Resolves user-facing text for the desktop UI.
 *
 * <p>Spanish is the base bundle ({@code messages.properties}); {@code messages_en.properties},
 * {@code messages_fr.properties}, … are added later without touching call sites (master prompt §36).
 * Domain and application code never carry literal strings — they carry {@link MessageKey}s that the
 * UI resolves here.
 */
public final class Messages {

    /** Package-visible so the key audit can load the same bundle this class does. */
    static final String BUNDLE = "org.sirmax.ui.i18n.messages";
    private static volatile ResourceBundle bundle = load(Locale.getDefault());

    private Messages() {}

    /** Switch the active locale (e.g. from a settings screen). */
    public static void setLocale(Locale locale) {
        bundle = load(locale);
    }

    public static Locale locale() {
        return bundle.getLocale();
    }

    /** Resolve a key, optionally interpolating {@code {0}}, {@code {1}} … arguments. */
    public static String get(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!"; // visible in dev, never a stack trace to the operator
        }
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    public static String get(MessageKey key, Object... args) {
        return get(key.value(), args);
    }

    private static ResourceBundle load(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE, locale);
    }
}
