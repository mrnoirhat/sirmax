// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.brand;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * The SIRMAX mark, loaded from the packaged PNG set.
 *
 * <p>Windows picks the size it needs from what a stage offers — 16px for the taskbar, 32px for
 * Alt-Tab, larger for the task view. Handing it a single large image and letting it downscale is
 * what makes an icon look muddy in the taskbar, so every size is a separately rendered file.
 *
 * <p>Missing files are tolerated: an icon is decoration, and a municipal counter losing its
 * window because a resource did not make it into the jar is a far worse failure than a blank one.
 */
public final class BrandMark {

    private static final List<Integer> SIZES = List.of(16, 32, 48, 128, 256);

    private BrandMark() {}

    /** Every available size, largest last. Empty when the resources are missing. */
    public static List<Image> icons() {
        List<Image> images = new ArrayList<>();
        for (int size : SIZES) {
            Image image = load(size);
            if (image != null) {
                images.add(image);
            }
        }
        return images;
    }

    /** Applies the mark to {@code stage}; a no-op when the resources are missing. */
    public static void apply(Stage stage) {
        stage.getIcons().addAll(icons());
    }

    /** The mark as a node sized to {@code edge} pixels, for use inside the UI. */
    public static Region node(double edge) {
        Region region = new Region();
        region.setMinSize(edge, edge);
        region.setPrefSize(edge, edge);
        region.setMaxSize(edge, edge);
        Image image = load(edge <= 32 ? 32 : edge <= 48 ? 48 : edge <= 128 ? 128 : 256);
        if (image != null) {
            region.setBackground(
                    new javafx.scene.layout.Background(
                            new javafx.scene.layout.BackgroundImage(
                                    image,
                                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                                    javafx.scene.layout.BackgroundPosition.CENTER,
                                    new javafx.scene.layout.BackgroundSize(
                                            edge, edge, false, false, false, false))));
        }
        return region;
    }

    private static Image load(int size) {
        try (InputStream in =
                BrandMark.class.getResourceAsStream("/org/sirmax/ui/brand/sirmax-" + size + ".png")) {
            return in == null ? null : new Image(in);
        } catch (Exception e) {
            return null;
        }
    }
}
