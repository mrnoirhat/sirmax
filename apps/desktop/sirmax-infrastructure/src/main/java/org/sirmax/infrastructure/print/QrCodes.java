// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.print;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;

/**
 * Renders the verification QR printed on official documents (master prompt §47).
 *
 * <p>Error-correction level M with a quiet zone of 2 modules: enough redundancy to survive a smudged
 * impact print without inflating the symbol past what fits on a 58 mm roll.
 *
 * <p>The QR carries only the verification code — never citizen data. §48 is explicit that public
 * verification must not expose private records, and a QR that anyone can photograph is as public as
 * data gets.
 */
final class QrCodes {

    private static final Map<EncodeHintType, Object> HINTS =
            Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8");

    /**
     * A black-and-white QR bitmap, or empty when the payload cannot be encoded.
     *
     * <p>Failing to draw a QR must never cost the citizen their receipt, so this returns empty
     * rather than throwing — the human-readable code beside it carries the same information.
     */
    Optional<BufferedImage> encode(String payload, int pixels) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            BitMatrix matrix =
                    new QRCodeWriter()
                            .encode(payload, BarcodeFormat.QR_CODE, pixels, pixels, HINTS);
            BufferedImage image =
                    new BufferedImage(
                            matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return Optional.of(image);
        } catch (WriterException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
