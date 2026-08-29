// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.infrastructure.print;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.sirmax.application.port.DocumentPrinter;
import org.sirmax.domain.document.PrinterProfile;
import org.sirmax.shared.SirmaxException;

/**
 * Prints through the Windows spooler via {@code javax.print} and PDFBox's printing support (master
 * prompt §59D).
 *
 * <p>Whatever printers Windows has drivers for — Epson impact, laser, thermal — appear here, because
 * this goes through the OS rather than talking to hardware. That is also why the printer name in a
 * {@link PrinterProfile} is the Windows queue name and not a device path.
 *
 * <p>Scaling is {@link Scaling#ACTUAL_SIZE}: a receipt or an invoice must come out at the size it
 * was laid out for. "Shrink to fit" would silently change the paper an invoice was designed for,
 * which is how a Letter invoice ends up printed 94% on A4 with the totals column clipped.
 */
public final class JavaPrintServiceDocumentPrinter implements DocumentPrinter {

    @Override
    public List<String> availablePrinters() {
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .map(PrintService::getName)
                .toList();
    }

    @Override
    public Optional<String> defaultPrinter() {
        return Optional.ofNullable(PrintServiceLookup.lookupDefaultPrintService())
                .map(PrintService::getName);
    }

    @Override
    public boolean print(byte[] pdf, PrinterProfile profile) {
        PrintService service = resolveService(profile);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            job.setPrintService(service);
            job.setPageable(
                    new PDFPageable(
                            document,
                            org.apache.pdfbox.printing.Orientation.AUTO,
                            false));
            job.setJobName("SIRMAX — " + profile.name());

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(new Copies(profile.copies()));
            attributes.add(new JobName("SIRMAX — " + profile.name(), null));

            // A silent profile is the whole point of configuring a counter receipt printer: the
            // cashier presses Imprimir and paper comes out (§59D).
            if (!profile.silent() && !job.printDialog(attributes)) {
                return false; // the operator cancelled; not an error
            }
            job.print(attributes);
            return true;
        } catch (IOException e) {
            throw new SirmaxException("Could not read the document to print", e);
        } catch (java.awt.print.PrinterException e) {
            throw new SirmaxException(
                    "The printer refused the job on profile " + profile.name(), e);
        }
    }

    /** The profile's queue, the system default, or a clear failure — never a silent wrong printer. */
    private PrintService resolveService(PrinterProfile profile) {
        if (profile.printerName().isPresent()) {
            String wanted = profile.printerName().get();
            return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                    .filter(s -> s.getName().equalsIgnoreCase(wanted))
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new SirmaxException(
                                            "Printer \"" + wanted + "\" is not available on this"
                                                    + " workstation"));
        }
        PrintService fallback = PrintServiceLookup.lookupDefaultPrintService();
        if (fallback == null) {
            throw new SirmaxException("This workstation has no default printer configured");
        }
        return fallback;
    }
}
