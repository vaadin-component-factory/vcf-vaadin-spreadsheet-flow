/**
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>} for the full
 * license.
 */
package com.vaadin.flow.component.spreadsheet;

import java.awt.Color;
import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.format.VCellFormat;
import org.apache.poi.ss.formula.ConditionalFormattingEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.VDataFormatter;

/**
 * POI library has two classes {@link org.apache.poi.ss.format.CellFormat} and
 * {@link org.apache.poi.ss.usermodel.DataFormatter} to deal with custom
 * formatting. The implementation is sometimes buggy!
 * <p>
 * This class work around the following bugs:
 * <p>
 * 1) If one of the custom format parts is literal (e.g. does not refer to the
 * number being entered), the formatting is not done correctly.
 * <p>
 * 2) Custom formats that have empty parts (i.e. they render a certain value as
 * empty) are not rendered correctly.
 * <p>
 * CellFormat does okay job for text formatting and literals (including empty
 * parts)
 * <p>
 * DataFormatter can correctly format numbers using the locale, but cannot
 * format text or literals.
 * <p>
 * This class tries to work around the most use cases by delegating a certain
 * case to one parser or another and changing the format string to be compatible
 * with the parser.
 */
class CustomDataFormatter extends VDataFormatter implements Serializable {
    // TODO: return parent class to DataFormatter once POI is fixed

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0#]+");

    // In a custom format the first part represents a format for positive
    // numbers the second for negative numbers, the third for zero and the
    // fourth a plain text
    private final int POSITIVE_FORMAT_INDEX = 0;
    private final int NEGATIVE_FORMAT_INDEX = 1;
    private final int ZERO_FORMAT_INDEX = 2;
    private final int TEXT_FORMAT_INDEX = 3;
    private Locale locale;

    public CustomDataFormatter() {
    }

    public CustomDataFormatter(Locale locale) {
        super(locale);
        this.locale = locale;
    }

    /**
     * This method delegates cell formatting to CellFormat if it's needed to
     * format a text or a literal, because CellFormat handles text formatting
     * much better than DataFormatter.
     * <p>
     * Otherwise use <code>DataFormatter#formatCellValue</code>
     **/
    @Override
    public String formatCellValue(Cell cell, FormulaEvaluator evaluator,
            ConditionalFormattingEvaluator cfEvaluator) {

        if (cell == null || cell.getCellStyle() == null) {
            return super.formatCellValue(cell, evaluator, cfEvaluator);
        }

        final String dataFormatString = cell.getCellStyle()
                .getDataFormatString();

        if (isGeneralFormat(dataFormatString)) {
            return super.formatCellValue(cell, evaluator, cfEvaluator);
        }

        final String[] parts = dataFormatString.split(";", -1);

        final CellType cellType = getCellType(cell, evaluator);

        if (cellType == CellType.NUMERIC) {
            return formatNumericValueUsingFormatPart(cell, evaluator,
                    cfEvaluator, parts);
        } else if (cellType == CellType.STRING && parts.length == 4) {
            return formatStringCellValue(cell, dataFormatString, parts);
        } else {
            return super.formatCellValue(cell, evaluator, cfEvaluator);
        }
    }

    @Override
    public void updateLocale(Locale newLocale) {
        super.updateLocale(newLocale);
        this.locale = newLocale;
    }

    private CellType getCellType(Cell cell, FormulaEvaluator evaluator) {

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = evaluator.evaluateFormulaCell(cell);
        }
        return cellType;
    }

    private String formatNumericValueUsingFormatPart(Cell cell,
            FormulaEvaluator evaluator,
            ConditionalFormattingEvaluator cfEvaluator, String[] formatParts) {

        final double value = cell.getNumericCellValue();
        final String format = getNumericFormat(value, formatParts);

        if (format.isEmpty()) {
            return "";
        }

        if (isOnlyLiteralFormat(format)) {
            // CellFormat can format literals correctly
            return formatTextUsingCellFormat(cell, format).text;
        } else {
            // DataFormatter can format numbers correctly
            return super.formatCellValue(cell, evaluator, cfEvaluator);
        }
    }

    private CellFormatResult formatTextUsingCellFormat(Cell cell,
            String format) {
        // TODO: replace this with a reference to CellFormat when moving back
        // to mainline Apache POI.
        return VCellFormat.getInstance(locale, format).apply(cell);
    }

    /**
     * Get the applicable text color for the cell. This uses Apache POI's
     * CellFormat logic, which parses and evaluates the cell's format string
     * against the cell's current value.
     * 
     * @param cell
     *            The cell to get the applicable custom formatting text color
     *            for.
     * @return a CSS color value string, or null if no text color should be
     *         applied.
     */
    public String getCellTextColor(Cell cell) {
        try {
            final String format = cell.getCellStyle().getDataFormatString();
            if (format == null || format.isEmpty() || isGeneralFormat(format)) {
                return null;
            }

            CellFormatResult result = formatTextUsingCellFormat(cell, format);

            if (result.textColor == null) {
                return null;
            }

            Color color = result.textColor; // AWT color value returned by POI

            // Convert calculated color value to simplest parseable hex string
            // @formatter:off
            final int cval = (color.getRed() << 16) | 
                (color.getGreen() << 8) | color.getBlue();
            final String hex = Integer.toHexString(cval);
            switch (hex.length()) {
                case 1: return "00000" + hex;
                case 2: return "0000" + hex;
                case 3: return "000" + hex;
                case 4: return "00" + hex;
                case 5: return "0" + hex;
                default: return hex;
            }
            // @formatter:on
        } catch (Exception e) {
            return null;
        }
    }

    private String getNumericFormat(double value, String[] formatParts) {
        // fall through intended
        switch (formatParts.length) {
        case 3:
        case 4:
            if (value == 0.0) {
                return formatParts[ZERO_FORMAT_INDEX];
            }
        case 2:
            if (value < 0.0) {
                return formatParts[NEGATIVE_FORMAT_INDEX];
            }
        case 1:
            if (value < 0.0) {
                return "-" + formatParts[POSITIVE_FORMAT_INDEX];
            }
        default:
            return formatParts[POSITIVE_FORMAT_INDEX];
        }
    }

    /**
     * Best attempt to check if the format contains numbers that we are
     * formatting or is purely literal. Known issue is that it does not consider
     * possible escaped/inside string characters, but it's a very rare case.
     */
    private boolean isOnlyLiteralFormat(String format) {
        return !NUMBER_PATTERN.matcher(format).find();
    }

    private boolean isGeneralFormat(String format) {
        return "General".equals(format);
    }

    /**
     * DataFormatter cannot format strings, but CellFormat can.
     */
    private String formatStringCellValue(Cell cell, String formatString,
            String[] parts) {
        if (parts[TEXT_FORMAT_INDEX].isEmpty()) {
            return "";
        }

        return formatTextUsingCellFormat(cell, formatString).text;
    }
}
