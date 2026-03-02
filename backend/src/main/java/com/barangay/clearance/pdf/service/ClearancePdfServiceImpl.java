package com.barangay.clearance.pdf.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.settings.entity.BarangaySettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDFBox 3.x implementation of {@link ClearancePdfService}.
 *
 * <p>
 * Generates an A4-sized barangay clearance certificate with:
 * <ol>
 * <li>Header — optional logo + Republic header + barangay info</li>
 * <li>Horizontal rule</li>
 * <li>Title — "BARANGAY CLEARANCE" (centered, bold, 16pt)</li>
 * <li>Metadata — clearance number, issuance date, validity</li>
 * <li>Body paragraph — resident details with text wrapping</li>
 * <li>Signature block — captain name and position</li>
 * </ol>
 * </p>
 *
 * <p>
 * Coordinate origin: bottom-left {@code (0, 0)}. Content is drawn top-down
 * by decrementing a {@code y} tracker.
 * </p>
 */
@Slf4j
@Service
public class ClearancePdfServiceImpl implements ClearancePdfService {

    // ── Page constants ──────────────────────────────────────────────────
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth(); // 595.28 pt
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 841.89 pt
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    // ── Font sizes ──────────────────────────────────────────────────────
    private static final float HEADER_FONT_SIZE = 10f;
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float META_FONT_SIZE = 10f;
    private static final float BODY_FONT_SIZE = 12f;
    private static final float SIGNATURE_FONT_SIZE = 11f;

    // ── Spacing ─────────────────────────────────────────────────────────
    private static final float LINE_SPACING = 1.4f;
    private static final float SECTION_GAP = 20f;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final int VALIDITY_MONTHS = 6;

    @Override
    public byte[] generate(ClearanceRequest clearance, Resident resident, BarangaySettings settings) {
        log.info("Generating PDF for clearance id={} number={}", clearance.getId(), clearance.getClearanceNumber());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            float y = PAGE_HEIGHT - MARGIN;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // 1. Header with logo + barangay info
                y = drawHeader(doc, cs, settings, fontRegular, fontBold, y);

                // 2. Horizontal rule
                y -= 10f;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= SECTION_GAP;

                // 3. Title
                y = drawCenteredText(cs, "BARANGAY CLEARANCE", fontBold, TITLE_FONT_SIZE, y);
                y -= SECTION_GAP;

                // 4. Metadata block
                y = drawMetadata(cs, clearance, fontRegular, fontBold, y);
                y -= SECTION_GAP;

                // 5. Body paragraph
                y = drawBody(cs, clearance, resident, settings, fontRegular, fontBold, y);
                y -= SECTION_GAP * 2;

                // 6. Signature block
                drawSignature(cs, settings, fontRegular, fontBold, y);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            byte[] pdfBytes = baos.toByteArray();
            log.info("PDF generated successfully, size={} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (IOException e) {
            log.error("Failed to generate PDF for clearance id={}", clearance.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Section renderers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Draws the header: optional logo on the left, Republic header and barangay
     * info centered.
     */
    private float drawHeader(PDDocument doc, PDPageContentStream cs, BarangaySettings settings,
            PDType1Font fontRegular, PDType1Font fontBold, float y) throws IOException {

        float logoWidth = 0;
        float logoHeight = 0;

        // Draw logo if present
        if (settings.getLogo() != null && settings.getLogo().length > 0) {
            try {
                PDImageXObject logo = PDImageXObject.createFromByteArray(doc, settings.getLogo(), "logo");
                logoHeight = Math.min(80f, logo.getHeight());
                logoWidth = logo.getWidth() * (logoHeight / logo.getHeight());

                // Position logo on the left side
                float logoX = MARGIN;
                float logoY = y - logoHeight;
                cs.drawImage(logo, logoX, logoY, logoWidth, logoHeight);
                log.debug("Logo embedded: {}x{} pt", logoWidth, logoHeight);
            } catch (IOException e) {
                log.warn("Failed to embed logo image, rendering text-only header", e);
                logoWidth = 0;
                logoHeight = 0;
            }
        }

        // Header text lines — centered on the page
        float headerY = y;

        headerY = drawCenteredText(cs, "Republika ng Pilipinas", fontItalic(fontRegular), HEADER_FONT_SIZE, headerY);
        headerY -= 2f;
        headerY = drawCenteredText(cs, "Province of " + settings.getProvince(), fontRegular, HEADER_FONT_SIZE, headerY);
        headerY -= 2f;
        headerY = drawCenteredText(cs, "Municipality of " + settings.getMunicipality(), fontRegular, HEADER_FONT_SIZE,
                headerY);
        headerY -= 4f;
        headerY = drawCenteredText(cs, settings.getBarangayName(), fontBold, 14f, headerY);

        // Return the lowest of logo bottom or text bottom
        return Math.min(headerY, y - logoHeight) - 5f;
    }

    /**
     * Draws the metadata block: clearance number, issuance date, validity.
     */
    private float drawMetadata(PDPageContentStream cs, ClearanceRequest clearance,
            PDType1Font fontRegular, PDType1Font fontBold, float y) throws IOException {

        LocalDate issuedDate = clearance.getIssuedAt() != null
                ? clearance.getIssuedAt().atZone(ZoneId.of("Asia/Manila")).toLocalDate()
                : LocalDate.now();
        LocalDate validUntil = issuedDate.plusMonths(VALIDITY_MONTHS);

        y = drawLabelValue(cs, "Clearance No.:", clearance.getClearanceNumber(), fontBold, fontRegular, META_FONT_SIZE,
                y);
        y -= 4f;
        y = drawLabelValue(cs, "Date Issued:", issuedDate.format(DATE_FMT), fontBold, fontRegular, META_FONT_SIZE, y);
        y -= 4f;
        y = drawLabelValue(cs, "Valid Until:", validUntil.format(DATE_FMT), fontBold, fontRegular, META_FONT_SIZE, y);

        return y;
    }

    /**
     * Draws the body paragraph with resident details and purpose.
     */
    private float drawBody(PDPageContentStream cs, ClearanceRequest clearance, Resident resident,
            BarangaySettings settings, PDType1Font fontRegular, PDType1Font fontBold,
            float y) throws IOException {

        String fullName = buildFullName(resident);
        int age = computeAge(resident.getBirthDate());
        String birthDateStr = resident.getBirthDate().format(DATE_FMT);
        String address = resident.getAddress();
        String barangayName = settings.getBarangayName();

        String purposeText = resolvePurposeText(clearance);

        // Introduction
        String intro = "TO WHOM IT MAY CONCERN:";
        y = drawLeftText(cs, intro, fontBold, BODY_FONT_SIZE, y);
        y -= BODY_FONT_SIZE * LINE_SPACING;

        // Body text
        String body = String.format(
                "This is to certify that %s, %d years old, born on %s, "
                        + "residing at %s, is a bona fide resident of %s "
                        + "and has no derogatory record on file in this office.",
                fullName, age, birthDateStr, address, barangayName);

        y = drawWrappedText(cs, body, fontRegular, BODY_FONT_SIZE, CONTENT_WIDTH, y);
        y -= BODY_FONT_SIZE * LINE_SPACING;

        // Purpose
        String purposeParagraph = String.format(
                "This clearance is issued upon the request of the above-named person for the purpose of: %s.",
                purposeText);

        y = drawWrappedText(cs, purposeParagraph, fontRegular, BODY_FONT_SIZE, CONTENT_WIDTH, y);

        return y;
    }

    /**
     * Draws the signature block: line, captain name, and position.
     */
    private void drawSignature(PDPageContentStream cs, BarangaySettings settings,
            PDType1Font fontRegular, PDType1Font fontBold, float y) throws IOException {

        // Signature line — right-aligned
        float sigBlockWidth = 200f;
        float sigBlockX = PAGE_WIDTH - MARGIN - sigBlockWidth;
        float sigCenterX = sigBlockX + sigBlockWidth / 2;

        // Signature line
        cs.setLineWidth(0.5f);
        cs.moveTo(sigBlockX, y);
        cs.lineTo(sigBlockX + sigBlockWidth, y);
        cs.stroke();

        // Captain name — centered below line
        y -= SIGNATURE_FONT_SIZE * LINE_SPACING;
        drawCenteredTextAt(cs, settings.getCaptainName().toUpperCase(), fontBold, SIGNATURE_FONT_SIZE, sigCenterX, y);

        // Title — centered below name
        y -= SIGNATURE_FONT_SIZE * LINE_SPACING;
        drawCenteredTextAt(cs, "Punong Barangay", fontRegular, SIGNATURE_FONT_SIZE - 1, sigCenterX, y);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Text helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Draws a single line of text centered on the page. Returns the new y position.
     */
    private float drawCenteredText(PDPageContentStream cs, String text,
            PDType1Font font, float fontSize, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2f;

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();

        return y - fontSize * LINE_SPACING;
    }

    /**
     * Draws text centered at a specific x coordinate.
     */
    private void drawCenteredTextAt(PDPageContentStream cs, String text,
            PDType1Font font, float fontSize, float centerX, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float x = centerX - textWidth / 2f;

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * Draws left-aligned text at the margin.
     */
    private float drawLeftText(PDPageContentStream cs, String text,
            PDType1Font font, float fontSize, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(text);
        cs.endText();
        return y - fontSize * LINE_SPACING;
    }

    /**
     * Draws a label-value pair, e.g. "Clearance No.: 2025-020001".
     */
    private float drawLabelValue(PDPageContentStream cs, String label, String value,
            PDType1Font labelFont, PDType1Font valueFont,
            float fontSize, float y) throws IOException {
        cs.beginText();
        cs.setFont(labelFont, fontSize);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(label + "  ");
        cs.setFont(valueFont, fontSize);
        cs.showText(value != null ? value : "—");
        cs.endText();
        return y - fontSize * LINE_SPACING;
    }

    /**
     * Wraps and draws text within the specified maxWidth.
     *
     * <p>
     * PDFBox has no built-in text wrapping. This method splits words and
     * measures each line with {@code font.getStringWidth()}.
     * </p>
     */
    private float drawWrappedText(PDPageContentStream cs, String text,
            PDType1Font font, float fontSize,
            float maxWidth, float y) throws IOException {

        List<String> lines = wrapText(text, font, fontSize, maxWidth);
        float leading = fontSize * LINE_SPACING;

        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(line);
            cs.endText();
            y -= leading;
        }

        return y;
    }

    /**
     * Splits text into lines that fit within the given width.
     *
     * @param text     the text to wrap
     * @param font     the font to measure
     * @param fontSize the font size
     * @param maxWidth the maximum line width in points
     * @return list of wrapped lines
     */
    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.isEmpty()
                    ? word
                    : currentLine + " " + word;
            float candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;

            if (candidateWidth > maxWidth && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Data helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds the resident's full name: "FirstName MiddleName LastName".
     * MiddleName is omitted if null/blank.
     */
    private String buildFullName(Resident resident) {
        StringBuilder sb = new StringBuilder();
        sb.append(resident.getFirstName());
        if (resident.getMiddleName() != null && !resident.getMiddleName().isBlank()) {
            sb.append(" ").append(resident.getMiddleName());
        }
        sb.append(" ").append(resident.getLastName());
        return sb.toString();
    }

    /**
     * Computes the resident's age from their birthdate.
     */
    private int computeAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Resolves the purpose text. For {@code OTHER}, uses the free-text description.
     */
    private String resolvePurposeText(ClearanceRequest clearance) {
        return switch (clearance.getPurpose()) {
            case EMPLOYMENT -> "Employment";
            case TRAVEL_ABROAD -> "Travel Abroad";
            case SCHOLARSHIP -> "Scholarship";
            case LOAN -> "Loan Application";
            case BUSINESS_PERMIT -> "Business Permit";
            case LEGAL -> "Legal Proceedings";
            case CEDULA -> "Cedula / Community Tax Certificate";
            case OTHER -> clearance.getPurposeOther() != null
                    ? clearance.getPurposeOther()
                    : "Personal";
        };
    }

    /**
     * Returns italic font variant. PDFBox Standard 14 fonts have a specific italic
     * variant.
     */
    private PDType1Font fontItalic(PDType1Font ignored) {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    }
}
