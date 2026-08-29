// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sirmax.ui.i18n.Messages;
import org.sirmax.ui.nav.RouteKey;
import org.sirmax.ui.nav.ShellNavigator;
import org.sirmax.ui.shell.ShellView;
import org.sirmax.ui.theme.Theme;

/**
 * The Phase 13 accessibility and UX audit (master prompt §36, §78).
 *
 * <p>Checks the properties that are cheap to verify and expensive to notice by eye: that every
 * control can be reached by keyboard, that nothing on screen is an untranslated key, and that no
 * message reaches an operator as a stack trace or a technical code.
 */
class AccessibilityAuditTest {

    @BeforeAll
    static void fx() {
        FxTestSupport.startToolkit();
    }

    private static ShellView shell() {
        ShellView shell = new ShellView(new ShellNavigator(RouteKey.HOME), Theme.LIGHT);
        Scene scene = new Scene(shell, 1200, 780);
        var stylesheet = ShellView.class.getResource("/org/sirmax/ui/theme/sirmax.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        shell.applyCss();
        shell.layout();
        return shell;
    }

    @Test
    void everyControlCanBeReachedByKeyboard() {
        List<String> unreachable =
                FxTestSupport.onFxThread(
                        () -> {
                            List<String> out = new ArrayList<>();
                            for (Node node : walk(shell())) {
                                boolean interactive =
                                        node instanceof Button || node instanceof TextInputControl;
                                // A visible control that cannot be tabbed to is unusable for
                                // anyone working without a mouse — and slow for everyone else.
                                if (interactive && node.isVisible() && !node.isFocusTraversable()) {
                                    out.add(node.getClass().getSimpleName() + " " + describe(node));
                                }
                            }
                            return out;
                        });

        assertThat(unreachable).as("visible controls that cannot be focused").isEmpty();
    }

    @Test
    void nothingOnScreenIsAnUnresolvedTranslationKey() {
        // Messages.get returns "!key!" for a missing entry rather than throwing, which keeps the
        // application usable — and makes a gap invisible unless something looks for it.
        List<String> missing =
                FxTestSupport.onFxThread(
                        () -> {
                            List<String> out = new ArrayList<>();
                            for (Node node : walk(shell())) {
                                String text = describe(node);
                                if (text.startsWith("!") && text.endsWith("!")) {
                                    out.add(text);
                                }
                            }
                            return out;
                        });

        assertThat(missing).as("untranslated strings on screen").isEmpty();
    }

    @Test
    void everyMessageKeyUsedByTheApplicationResolves() {
        // The shell only renders a fraction of the catalogue. This checks the whole bundle is
        // internally consistent: a key that exists must have text, in every locale shipped.
        var bundle =
                java.util.ResourceBundle.getBundle(
                        "org.sirmax.ui.i18n.messages", java.util.Locale.of("es"));
        List<String> empty = new ArrayList<>();
        for (String key : java.util.Collections.list(bundle.getKeys())) {
            if (bundle.getString(key).isBlank()) {
                empty.add(key);
            }
        }
        assertThat(empty).as("message keys with no text").isEmpty();
        assertThat(java.util.Collections.list(bundle.getKeys())).hasSizeGreaterThan(200);
    }

    @Test
    void errorMessagesNeverLeakTechnicalDetail() {
        // §78: technical errors are hidden from the end user. Every message an operator can see
        // has to read as Spanish, not as a class name or a SQL fragment.
        var bundle =
                java.util.ResourceBundle.getBundle(
                        "org.sirmax.ui.i18n.messages", java.util.Locale.of("es"));
        List<String> leaks = new ArrayList<>();
        for (String key : java.util.Collections.list(bundle.getKeys())) {
            String text = bundle.getString(key);
            if (text.contains("Exception")
                    || text.contains("org.sirmax")
                    || text.contains("SELECT ")
                    || text.contains("null")) {
                leaks.add(key + " = " + text);
            }
        }
        assertThat(leaks).as("messages exposing technical detail").isEmpty();
    }

    @Test
    void everyNavigationDestinationHasATitle() {
        var titles = FxTestSupport.onFxThread(() -> shell().routeTitles());

        assertThat(titles.values()).allSatisfy(key -> assertThat(key).isNotBlank());
        assertThat(titles.values())
                .allSatisfy(key -> assertThat(Messages.get(key)).doesNotStartWith("!"));
    }

    private static List<Node> walk(Parent root) {
        List<Node> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private static void collect(Parent parent, List<Node> out) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            out.add(child);
            if (child instanceof Parent nested) {
                collect(nested, out);
            }
        }
    }

    /** Whatever text this node shows an operator: its label, or its placeholder. */
    private static String describe(Node node) {
        if (node instanceof javafx.scene.control.Labeled labeled) {
            return labeled.getText() == null ? "" : labeled.getText();
        }
        if (node instanceof TextInputControl input) {
            return input.getPromptText() == null ? "" : input.getPromptText();
        }
        return "";
    }
}
