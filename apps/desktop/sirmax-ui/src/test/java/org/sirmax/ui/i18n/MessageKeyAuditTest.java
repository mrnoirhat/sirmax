// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sirmax.domain.backup.BackupKind;
import org.sirmax.domain.backup.BackupSchedule;
import org.sirmax.domain.document.DocumentKind;
import org.sirmax.domain.document.PaperFormat;
import org.sirmax.domain.finance.ChargeType;
import org.sirmax.domain.finance.InvoiceStatus;
import org.sirmax.domain.finance.Payment;
import org.sirmax.domain.finance.PaymentMethod;
import org.sirmax.domain.procedure.ProcedureEventKind;
import org.sirmax.domain.procedure.ProcedureStatus;
import org.sirmax.domain.service.ServiceStatus;
import org.sirmax.domain.service.ServiceType;
import org.sirmax.domain.service.Sla;
import org.sirmax.domain.workflow.TransitionKind;

/**
 * Every message key the UI asks for must exist in the bundle.
 *
 * <p>A missing key does not crash: {@link Messages} falls back to showing the key itself, which is
 * the right runtime behaviour and exactly why the mistake survives review — {@code services.publish}
 * on a button looks like a label until someone reads it closely. Nothing else in the build checks
 * this, so a typo in a key shipped silently.
 */
class MessageKeyAuditTest {

    private static final Path SOURCES = Path.of("src", "main", "java");

    /**
     * The calls that take a message key as their first string argument.
     *
     * <p>Deliberately narrow: matching every dotted string literal would sweep up audit sources
     * ({@code "desktop.billing"}), permission codes and system properties, and an audit that has to
     * be taught a growing list of exceptions stops being trusted.
     */
    private static final List<Pattern> CALLS =
            Stream.of(
                            "Messages\\.get",
                            "Typography\\.(?:display|title|subtitle|body|muted)",
                            "Buttons\\.(?:primary|secondary|danger|ghost)",
                            "new FormField",
                            "FormField\\.withLiteralLabel",
                            "new PlaceholderView\\s*\\(\\s*RouteKey\\.[A-Z_]+\\s*,",
                            "toasts\\.(?:info|success|warning|error)",
                            "Dialogs\\.(?:info|error)")
                    .map(call -> Pattern.compile(call + "\\s*\\(\\s*\"([a-z][A-Za-z0-9_.]*)\""))
                    .toList();

    /** Enum labels are built at runtime, so the regex above cannot see them. */
    private static final Map<String, Class<? extends Enum<?>>> ENUM_PREFIXES =
            Map.ofEntries(
                    Map.entry("service.type", ServiceType.class),
                    Map.entry("service.status", ServiceStatus.class),
                    Map.entry("charge.type", ChargeType.class),
                    Map.entry("document.kind", DocumentKind.class),
                    Map.entry("paper.format", PaperFormat.class),
                    Map.entry("backup.kind", BackupKind.class),
                    Map.entry("backup.frequency", BackupSchedule.Frequency.class),
                    Map.entry("services.sla_basis", Sla.Basis.class),
                    Map.entry("payment.method", PaymentMethod.class),
                    Map.entry("procedure.status", ProcedureStatus.class),
                    Map.entry("invoice.status", InvoiceStatus.class),
                    Map.entry("payment.status", Payment.Status.class),
                    Map.entry("procedure.event", ProcedureEventKind.class),
                    Map.entry("procedure.transition", TransitionKind.class));

    @Test
    void everyKeyTheUiAsksForExistsInTheBundle() throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle(Messages.BUNDLE, new Locale("es"));
        Set<String> missing = new TreeSet<>();

        for (String key : keysReferencedInSource()) {
            if (!bundle.containsKey(key)) {
                missing.add(key);
            }
        }

        assertThat(missing)
                .as("message keys used by the UI but absent from messages.properties")
                .isEmpty();
    }

    @Test
    void everyEnumConstantShownToAnOperatorHasALabel() {
        ResourceBundle bundle = ResourceBundle.getBundle(Messages.BUNDLE, new Locale("es"));
        Set<String> missing = new TreeSet<>();

        ENUM_PREFIXES.forEach(
                (prefix, type) -> {
                    for (Enum<?> constant : type.getEnumConstants()) {
                        String key = prefix + "." + constant.name().toLowerCase(Locale.ROOT);
                        if (!bundle.containsKey(key)) {
                            missing.add(key);
                        }
                    }
                });

        assertThat(missing)
                .as("enum constants rendered by Enums#label with no translation")
                .isEmpty();
    }

    private static Set<String> keysReferencedInSource() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                for (Pattern pattern : CALLS) {
                    Matcher matcher = pattern.matcher(source);
                    while (matcher.find()) {
                        keys.add(matcher.group(1));
                    }
                }
            }
        }
        // A guard on the guard: if a refactor moves the sources or breaks the patterns, this test
        // would pass by finding nothing, which is the failure mode an audit must not have.
        assertThat(keys).hasSizeGreaterThan(100);
        return keys;
    }
}
