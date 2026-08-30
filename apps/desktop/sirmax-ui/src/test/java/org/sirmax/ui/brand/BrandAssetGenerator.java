// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.ui.brand;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Draws the SIRMAX mark and writes it out for every surface that needs it.
 *
 * <p>The geometry lives here rather than in an SVG that gets rasterised by some
 * tool-of-the-day, because the assets have to be reproducible on a machine with
 * nothing installed but a JDK — the same constraint the rest of the build works
 * under. {@code brand/sirmax-logo.svg} carries the identical shapes for the web,
 * where an SVG is what you want.
 *
 * <p>Off by default; it writes outside the build directory.
 *
 * <pre>{@code
 * ./gradlew :sirmax-ui:test --tests "*BrandAssetGenerator*" -Dsirmax.brand=true
 * }</pre>
 */
@EnabledIfSystemProperty(named = "sirmax.brand", matches = "true")
class BrandAssetGenerator {

    /** The mark is designed on a 256-unit grid; every size scales from it. */
    private static final int GRID = 256;

    private static final Color SEAL = new Color(0x1D4ED8);
    private static final Color SHEET = Color.WHITE;
    private static final Color FOLD = new Color(0x93C5FD);
    private static final Color RULE = new Color(0xC7D7F5);

    private static final Path REPO = Path.of("..", "..", "..").normalize();

    /**
     * Windows shows the small sizes in the taskbar and the large ones in Explorer;
     * an icon that only ships 256px gets scaled down badly at 16px.
     */
    private static final List<Integer> ICO_SIZES = List.of(16, 24, 32, 48, 64, 128, 256);

    @Test
    void writeEveryAsset() throws IOException {
        // Desktop: a multi-resolution .ico is what jpackage wants.
        Path desktop = REPO.resolve("apps/desktop/sirmax-app/src/main/resources/org/sirmax/app");
        Files.createDirectories(desktop);
        Files.write(desktop.resolve("sirmax.ico"), ico(ICO_SIZES));

        // JavaFX cannot load .ico, so the window icon is a PNG set.
        Path uiIcons = REPO.resolve("apps/desktop/sirmax-ui/src/main/resources/org/sirmax/ui/brand");
        Files.createDirectories(uiIcons);
        for (int size : List.of(16, 32, 48, 128, 256)) {
            write(uiIcons.resolve("sirmax-" + size + ".png"), size);
        }

        // Web: the SVG is the source of truth, but a PNG favicon still matters for
        // the browsers and crawlers that ignore it.
        for (String app : List.of("apps/landing/public", "apps/docs/static/img")) {
            Path dir = REPO.resolve(app);
            Files.createDirectories(dir);
            write(dir.resolve("logo-512.png"), 512);
            write(dir.resolve("apple-touch-icon.png"), 180);
            Files.write(dir.resolve("favicon.ico"), ico(List.of(16, 32, 48)));
            Files.copy(
                    REPO.resolve("brand/sirmax-logo.svg"),
                    dir.resolve("logo.svg"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Brand assets written from " + REPO.toAbsolutePath());
    }

    private static void write(Path target, int size) throws IOException {
        ImageIO.write(render(size), "png", target.toFile());
    }

    /** The mark at {@code size}px, transparent outside the seal. */
    static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double s = size / (double) GRID;
        g.scale(s, s);

        // Seal
        g.setColor(SEAL);
        g.fill(new RoundRectangle2D.Double(16, 16, 224, 224, 96, 96));

        // The record: a sheet with a folded corner.
        GeneralPath sheet = new GeneralPath(Path2D.WIND_NON_ZERO);
        sheet.moveTo(78, 62);
        sheet.lineTo(152, 62);
        sheet.lineTo(182, 92);
        sheet.lineTo(182, 194);
        sheet.quadTo(182, 204, 172, 204);
        sheet.lineTo(78, 204);
        sheet.quadTo(68, 204, 68, 194);
        sheet.lineTo(68, 72);
        sheet.quadTo(68, 62, 78, 62);
        sheet.closePath();
        g.setColor(SHEET);
        g.fill(sheet);

        GeneralPath fold = new GeneralPath();
        fold.moveTo(152, 62);
        fold.lineTo(182, 92);
        fold.lineTo(152, 92);
        fold.closePath();
        g.setColor(FOLD);
        g.fill(fold);

        // Two lines of record, then the register stamp. Everything stays inside
        // the sheet: a band that crosses the edge reads as a rendering mistake at
        // icon sizes, not as a stamp.
        g.setColor(RULE);
        g.fill(new RoundRectangle2D.Double(92, 110, 64, 10, 10, 10));
        g.fill(new RoundRectangle2D.Double(92, 128, 44, 10, 10, 10));
        g.setColor(SEAL);
        g.fill(new RoundRectangle2D.Double(92, 152, 66, 20, 20, 20));

        g.setStroke(new BasicStroke(0));
        g.dispose();
        return image;
    }

    /**
     * A Windows .ico containing PNG-encoded images.
     *
     * <p>Written by hand because the format is a 6-byte header plus a 16-byte
     * directory entry per image — less code than pulling in a dependency, and one
     * fewer thing to keep up to date.
     */
    static byte[] ico(List<Integer> sizes) throws IOException {
        List<byte[]> pngs = new java.util.ArrayList<>();
        for (int size : sizes) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(render(size), "png", out);
            pngs.add(out.toByteArray());
        }

        int headerSize = 6 + 16 * pngs.size();
        int total = headerSize + pngs.stream().mapToInt(p -> p.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short) 0); // reserved
        buffer.putShort((short) 1); // type: icon
        buffer.putShort((short) pngs.size());

        int offset = headerSize;
        for (int i = 0; i < pngs.size(); i++) {
            int size = sizes.get(i);
            // 256 is encoded as 0 — the field is a single byte.
            buffer.put((byte) (size >= 256 ? 0 : size)); // width
            buffer.put((byte) (size >= 256 ? 0 : size)); // height
            buffer.put((byte) 0); // palette entries: none, it is truecolour
            buffer.put((byte) 0); // reserved
            buffer.putShort((short) 1); // colour planes
            buffer.putShort((short) 32); // bits per pixel
            buffer.putInt(pngs.get(i).length);
            buffer.putInt(offset);
            offset += pngs.get(i).length;
        }
        for (byte[] png : pngs) {
            buffer.put(png);
        }
        return buffer.array();
    }
}
