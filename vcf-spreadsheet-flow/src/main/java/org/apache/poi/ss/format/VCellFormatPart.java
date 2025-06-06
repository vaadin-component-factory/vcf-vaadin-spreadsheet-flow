/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.ss.format;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.util.CodepointsUtil;
import org.apache.poi.util.LocaleUtil;

import javax.swing.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.poi.ss.format.CellFormatter.quote;

/*
 * Note: This file has been overridden by Vaadin to allow proper formatting
 * of all available colors. This override should be removed once the color
 * formatting has been fixed upstream in Apache POI.
 * 
 * This class has been heavily modified in order to provide full support
 * for colors supported by Excel.
 * 
 * As of February 2025, Apache POI's implementation of this class does not
 * provide Excel-correct color values, does not support indexed color at all
 * ánd is missing the definition for magenta completely.
 * 
 * TODO: when upgrading Apache POI, check to see if this class has been
 *       updated with correct color support!
 */

/**
 * Objects of this class represent a single part of a cell format expression.
 * Each cell can have up to four of these for positive, zero, negative, and text
 * values.
 * <p>
 * Each format part can contain a color, a condition, and will always contain a
 * format specification.  For example {@code "[Red][>=10]#"} has a color
 * ({@code [Red]}), a condition ({@code >=10}) and a format specification
 * ({@code #}).
 * <p>
 * This class also contains patterns for matching the subparts of format
 * specification.  These are used internally, but are made public in case other
 * code has use for them.
 */
@SuppressWarnings("RegExpRepeatedSpace")
public class VCellFormatPart {
    private static final Logger LOG = LogManager.getLogger(VCellFormatPart.class);

    static final Map<String, Color> NAMED_COLORS;
    static final List<Color> INDEXED_COLORS;

    private final Color color;
    private final CellFormatCondition condition;
    private final VCellFormatter format;
    private final VCellFormatType type;

    /** Pattern for the color part of a cell format part. */
    public static final Pattern COLOR_PAT;
    /** Pattern for the condition part of a cell format part. */
    public static final Pattern CONDITION_PAT;
    /** Pattern for the format specification part of a cell format part. */
    public static final Pattern SPECIFICATION_PAT;
    /** Pattern for the currency symbol part of a cell format part */
    public static final Pattern CURRENCY_PAT;
    /** Pattern for an entire cell single part. */
    public static final Pattern FORMAT_PAT;

    /** Within {@link #FORMAT_PAT}, the group number for the matched color. */
    public static final int COLOR_GROUP;

    /**
     * Within {@link #FORMAT_PAT}, the group number for the operator in the
     * condition.
     */
    public static final int CONDITION_OPERATOR_GROUP;
    /**
     * Within {@link #FORMAT_PAT}, the group number for the value in the
     * condition.
     */
    public static final int CONDITION_VALUE_GROUP;
    /**
     * Within {@link #FORMAT_PAT}, the group number for the format
     * specification.
     */
    public static final int SPECIFICATION_GROUP;

    static {
        // Build indexed color list based on this table
        // https://www.excelsupersite.com/what-are-the-56-colorindex-colors-in-excel/
        INDEXED_COLORS = List.of(
            new Color(0x000000),      // Color 1 / black
            new Color(0xFFFFFF),      // Color 2 / white
            new Color(0xFF0000),      // Color 3 / red
            new Color(0x00FF00),      // Color 4 / green
            new Color(0x0000FF),      // Color 5 / blue
            new Color(0xFFFF00),      // Color 6 / yellow
            new Color(0xFF00FF),      // Color 7 / magenta
            new Color(0x00FFFF),      // Color 8 / cyan
            new Color(0x800000),      // Color 9
            new Color(0x008000),      // Color 10
            new Color(0x000080),      // Color 11
            new Color(0x808000),      // Color 12
            new Color(0x800080),      // Color 13
            new Color(0x008080),      // Color 14
            new Color(0xC0C0C0),      // Color 15
            new Color(0x808080),      // Color 16
            new Color(0x9999FF),      // Color 17
            new Color(0x993366),      // Color 18
            new Color(0xFFFFCC),      // Color 19
            new Color(0xCCFFFF),      // Color 20
            new Color(0x660066),      // Color 21
            new Color(0xFF8080),      // Color 22
            new Color(0x0066CC),      // Color 23
            new Color(0xCCCCFF),      // Color 24
            new Color(0x000080),      // Color 25
            new Color(0xFF00FF),      // Color 26
            new Color(0xFFFF00),      // Color 27
            new Color(0x00FFFF),      // Color 28
            new Color(0x800080),      // Color 29
            new Color(0x800000),      // Color 30
            new Color(0x008080),      // Color 31
            new Color(0x0000FF),      // Color 32
            new Color(0x00CCFF),      // Color 33
            new Color(0xCCFFFF),      // Color 34
            new Color(0xCCFFCC),      // Color 35
            new Color(0xFFFF99),      // Color 36
            new Color(0x99CCFF),      // Color 37
            new Color(0xFF99CC),      // Color 38
            new Color(0xCC99FF),      // Color 39
            new Color(0xFFCC99),      // Color 40
            new Color(0x3366FF),      // Color 41
            new Color(0x33CCCC),      // Color 42
            new Color(0x99CC00),      // Color 43
            new Color(0xFFCC00),      // Color 44
            new Color(0xFF9900),      // Color 45
            new Color(0xFF6600),      // Color 46
            new Color(0x666699),      // Color 47
            new Color(0x969696),      // Color 48
            new Color(0x003366),      // Color 49
            new Color(0x339966),      // Color 50
            new Color(0x003300),      // Color 51
            new Color(0x333300),      // Color 52
            new Color(0x993300),      // Color 53
            new Color(0x993366),      // Color 54
            new Color(0x333399),      // Color 55
            new Color(0x333333)       // Color 56
        );
        
        // Build named color list based on HSSFColorPredefined, just as
        // Apache POI does it originally. This gives us a wider range
        // of acceptable colors, but also puts out outside the acceptable
        // color range of Excel.
        NAMED_COLORS = new TreeMap<>(
            String.CASE_INSENSITIVE_ORDER);
            
         for (HSSFColor.HSSFColorPredefined color : HSSFColor.HSSFColorPredefined.values()) {
             String name = color.name().toLowerCase();
             short[] rgb = color.getTriplet();
             Color c = new Color(rgb[0], rgb[1], rgb[2]);
             NAMED_COLORS.put(name, c);
             if (name.indexOf("_percent") > 0) {
                NAMED_COLORS.put(name.replace("_percent", "%")
                    .replaceAll("\\_", " "), c);
             }
             if (name.indexOf('_') > 0) {
                 NAMED_COLORS.put(name.replaceAll("\\_", " "), c);
             }
        }

        // Replace the standard colors with standard values
        // to support Excel like functionality
        NAMED_COLORS.put("black", INDEXED_COLORS.get(0));
        NAMED_COLORS.put("white", INDEXED_COLORS.get(1));
        NAMED_COLORS.put("red", INDEXED_COLORS.get(2));
        NAMED_COLORS.put("green", INDEXED_COLORS.get(3));
        NAMED_COLORS.put("blue", INDEXED_COLORS.get(4));
        NAMED_COLORS.put("yellow", INDEXED_COLORS.get(5));
        NAMED_COLORS.put("magenta", INDEXED_COLORS.get(6));
        NAMED_COLORS.put("cyan", INDEXED_COLORS.get(7));

        // A condition specification
        String condition = "([<>=]=?|!=|<>)    # The operator\n" +
                "  \\s*(-?([0-9]+(?:\\.[0-9]*)?)|(\\.[0-9]*))\\s*  # The constant to test against\n";

        // A currency symbol / string, in a specific locale
        String currency = "(\\[\\$.{0,3}(-[0-9a-f]{3,4})?])";

        // A number specification
        // Note: careful that in something like ##, that the trailing comma is not caught up in the integer part

        // A part of a specification
        //noinspection RegExpRedundantEscape
        String part = "\\\\.                     # Quoted single character\n" +
                "|\"([^\\\\\"]|\\\\.)*\"         # Quoted string of characters (handles escaped quotes like \\\") \n" +
                "|"+currency+"                   # Currency symbol in a given locale\n" +
                "|_.                             # Space as wide as a given character\n" +
                "|\\*.                           # Repeating fill character\n" +
                "|@                              # Text: cell text\n" +
                "|([0?\\#][0?\\#,]*)             # Number: digit + other digits and commas\n" +
                "|e[-+]                          # Number: Scientific: Exponent\n" +
                "|m{1,5}                         # Date: month or minute spec\n" +
                "|d{1,4}                         # Date: day/date spec\n" +
                "|y{2,4}                         # Date: year spec\n" +
                "|h{1,2}                         # Date: hour spec\n" +
                "|s{1,2}                         # Date: second spec\n" +
                "|am?/pm?                        # Date: am/pm spec\n" +
                "|\\[h{1,2}]                     # Elapsed time: hour spec\n" +
                "|\\[m{1,2}]                     # Elapsed time: minute spec\n" +
                "|\\[s{1,2}]                     # Elapsed time: second spec\n" +
                "|[^;]                           # A character\n" + "";

        // Build the color code matching expression.
        // We should match any named color in the set as well as a string in the form
        // of "Color 8" or "Color 15".
        String color = "\\[(";
        for (String key : NAMED_COLORS.keySet()) {
            // Escape special characters in the color name
            color += key.replaceAll("([^a-zA-Z0-9])", "\\\\$1") + "|";
        }
        // Match the indexed color table
        color += "color\\ [0-9]+)\\]";

        String format = "(?:" + color + ")?               # Text color\n" +
                "(?:\\[" + condition + "])?               # Condition\n" +
                // see https://msdn.microsoft.com/en-ca/goglobal/bb964664.aspx and https://bz.apache.org/ooo/show_bug.cgi?id=70003
                // we ignore these for now though
                "(?:\\[\\$-[0-9a-fA-F]+])?                # Optional locale id, ignored currently\n" +
                "((?:" + part + ")+)                      # Format spec\n";

        int flags = Pattern.COMMENTS | Pattern.CASE_INSENSITIVE;
        COLOR_PAT = Pattern.compile(color, flags);
        CONDITION_PAT = Pattern.compile(condition, flags);
        SPECIFICATION_PAT = Pattern.compile(part, flags);
        CURRENCY_PAT = Pattern.compile(currency, flags);
        FORMAT_PAT = Pattern.compile(format, flags);

        // Calculate the group numbers of important groups. (They shift around
        // when the pattern is changed; this way we figure out the numbers by
        // experimentation.)

        COLOR_GROUP = findGroup(FORMAT_PAT, "[Blue]@", "Blue");
        CONDITION_OPERATOR_GROUP = findGroup(FORMAT_PAT, "[>=1]@", ">=");
        CONDITION_VALUE_GROUP = findGroup(FORMAT_PAT, "[>=1]@", "1");
        SPECIFICATION_GROUP = findGroup(FORMAT_PAT, "[Blue][>1]\\a ?", "\\a ?");

        // Once patterns have been compiled, add indexed colors to
        // NAMED_COLORS so they can be easily picked up by an unmodified 
        // getColor() implementation
        for (int i = 0; i < INDEXED_COLORS.size(); ++i) {
            NAMED_COLORS.put("color " + (i + 1), INDEXED_COLORS.get(i));
        }
        // NOTE: the INDEXED_COLORS list is retained for future use, even
        //       though it is not currently utilized outside the static
        //       initialization logic.
    }

    interface PartHandler {
        String handlePart(Matcher m, String part, VCellFormatType type,
                StringBuffer desc);
    }

    /**
     * Create an object to represent a format part.
     *
     * @param desc The string to parse.
     */
    public VCellFormatPart(String desc) {
        this(LocaleUtil.getUserLocale(), desc);
    }

    /**
     * Create an object to represent a format part.
     *
     * @param locale The locale to use.
     * @param desc The string to parse.
     */
    public VCellFormatPart(Locale locale, String desc) {
        Matcher m = FORMAT_PAT.matcher(desc);
        if (!m.matches()) {
            throw new IllegalArgumentException("Unrecognized format: " + quote(
                    desc));
        }
        color = getColor(m);
        condition = getCondition(m);
        type = getCellFormatType(m);
        format = getFormatter(locale, m);
    }

    /**
     * Returns {@code true} if this format part applies to the given value. If
     * the value is a number and this is part has a condition, returns
     * {@code true} only if the number passes the condition.  Otherwise, this
     * always return {@code true}.
     *
     * @param valueObject The value to evaluate.
     *
     * @return {@code true} if this format part applies to the given value.
     */
    public boolean applies(Object valueObject) {
        if (condition == null || !(valueObject instanceof Number)) {
            if (valueObject == null)
                throw new NullPointerException("valueObject");
            return true;
        } else {
            Number num = (Number) valueObject;
            return condition.pass(num.doubleValue());
        }
    }

    /**
     * Returns the number of the first group that is the same as the marker
     * string. Starts from group 1.
     *
     * @param pat    The pattern to use.
     * @param str    The string to match against the pattern.
     * @param marker The marker value to find the group of.
     *
     * @return The matching group number.
     *
     * @throws IllegalArgumentException No group matches the marker.
     */
    private static int findGroup(Pattern pat, String str, String marker) {
        Matcher m = pat.matcher(str);
        if (!m.find())
            throw new IllegalArgumentException(
                    "Pattern \"" + pat.pattern() + "\" doesn't match \"" + str +
                            "\"");
        for (int i = 1; i <= m.groupCount(); i++) {
            String grp = m.group(i);
            if (grp != null && grp.equals(marker))
                return i;
        }
        throw new IllegalArgumentException(
                "\"" + marker + "\" not found in \"" + pat.pattern() + "\"");
    }

    /**
     * Returns the color specification from the matcher, or {@code null} if
     * there is none.
     *
     * @param m The matcher for the format part.
     *
     * @return The color specification or {@code null}.
     */
    private static Color getColor(Matcher m) {
        String cdesc = m.group(COLOR_GROUP);

        if (cdesc == null || cdesc.length() == 0) {
            return null;
        }

        Color c = NAMED_COLORS.get(cdesc);
        if (c == null) {
            LOG.error("Unknown color: {}", quote(cdesc));
        }
        return c;
    }

    /**
     * Returns the condition specification from the matcher, or {@code null} if
     * there is none.
     *
     * @param m The matcher for the format part.
     *
     * @return The condition specification or {@code null}.
     */
    private CellFormatCondition getCondition(Matcher m) {
        String mdesc = m.group(CONDITION_OPERATOR_GROUP);
        if (mdesc == null || mdesc.length() == 0)
            return null;
        return CellFormatCondition.getInstance(m.group(
                CONDITION_OPERATOR_GROUP), m.group(CONDITION_VALUE_GROUP));
    }

    /**
     * Returns the VCellFormatType object implied by the format specification for
     * the format part.
     *
     * @param matcher The matcher for the format part.
     *
     * @return The VCellFormatType.
     */
    private VCellFormatType getCellFormatType(Matcher matcher) {
        String fdesc = matcher.group(SPECIFICATION_GROUP);
        return formatType(fdesc);
    }

    /**
     * Returns the formatter object implied by the format specification for the
     * format part.
     *
     * @param matcher The matcher for the format part.
     *
     * @return The formatter.
     */
    private VCellFormatter getFormatter(Locale locale, Matcher matcher) {
        String fdesc = matcher.group(SPECIFICATION_GROUP);

        // For now, we don't support localised currencies, so simplify if there
        Matcher currencyM = CURRENCY_PAT.matcher(fdesc);
        if (currencyM.find()) {
            String currencyPart = currencyM.group(1);
            String currencyRepl;
            if (currencyPart.startsWith("[$-")) {
                // Default $ in a different locale
                currencyRepl = "$";
            } else if (!currencyPart.contains("-")) {
                // Accounting formats such as USD [$USD]
                currencyRepl = currencyPart.substring(2, currencyPart.indexOf("]"));
            } else {
                currencyRepl = currencyPart.substring(2, currencyPart.lastIndexOf('-'));
            }
            fdesc = fdesc.replace(currencyPart, currencyRepl);
        }

        // Build a formatter for this simplified string
        return type.formatter(locale, fdesc);
    }

    /**
     * Returns the type of format.
     *
     * @param fdesc The format specification
     *
     * @return The type of format.
     */
    private VCellFormatType formatType(String fdesc) {
        fdesc = fdesc.trim();
        if (fdesc.isEmpty() || fdesc.equalsIgnoreCase("General"))
            return VCellFormatType.GENERAL;

        Matcher m = SPECIFICATION_PAT.matcher(fdesc);
        boolean couldBeDate = false;
        boolean seenZero = false;
        while (m.find()) {
            String repl = m.group(0);
            Iterator<String> codePoints = CodepointsUtil.iteratorFor(repl);
            if (codePoints.hasNext()) {
                String c1 = codePoints.next();

                switch (c1) {
                case "@":
                    return VCellFormatType.TEXT;
                case "d":
                case "D":
                case "y":
                case "Y":
                    return VCellFormatType.DATE;
                case "h":
                case "H":
                case "m":
                case "M":
                case "s":
                case "S":
                    // These can be part of date, or elapsed
                    couldBeDate = true;
                    break;
                case "0":
                    // This can be part of date, elapsed, or number
                    seenZero = true;
                    break;
                case "[":
                    String c2 = null;
                    if (codePoints.hasNext())
                        c2 = codePoints.next().toLowerCase(Locale.ROOT);
                    if ("h".equals(c2) || "m".equals(c2) || "s".equals(c2)) {
                        return VCellFormatType.ELAPSED;
                    }
                    if ("$".equals(c2)) {
                        // Localised currency
                        return VCellFormatType.NUMBER;
                    }
                    // Something else inside [] which isn't supported!
                    throw new IllegalArgumentException("Unsupported [] format block '" +
                                                       repl + "' in '" + fdesc + "' with c2: " + c2);
                case "#":
                case "?":
                    return VCellFormatType.NUMBER;
                }
            }
        }

        // Nothing definitive was found, so we figure out it deductively
        if (couldBeDate)
            return VCellFormatType.DATE;
        if (seenZero)
            return VCellFormatType.NUMBER;
        return VCellFormatType.TEXT;
    }

    /**
     * Returns a version of the original string that has any special characters
     * quoted (or escaped) as appropriate for the cell format type.  The format
     * type object is queried to see what is special.
     *
     * @param repl The original string.
     * @param type The format type representation object.
     *
     * @return A version of the string with any special characters replaced.
     *
     * @see VCellFormatType#isSpecial(char)
     */
    static String quoteSpecial(String repl, VCellFormatType type) {
        StringBuilder sb = new StringBuilder();
        PrimitiveIterator.OfInt codePoints = CodepointsUtil.primitiveIterator(repl);

        int codepoint;
        while (codePoints.hasNext()) {
            codepoint = codePoints.nextInt();
            if (codepoint == '\'' && type.isSpecial('\'')) {
                sb.append('\u0000');
                continue;
            }

            char[] chars = Character.toChars(codepoint);
            boolean special = type.isSpecial(chars[0]);
            if (special)
                sb.append('\'');
            sb.append(chars);
            if (special)
                sb.append('\'');
        }
        return sb.toString();
    }

    /**
     * Apply this format part to the given value.  This returns a {@link
     * CellFormatResult} object with the results.
     *
     * @param value The value to apply this format part to.
     *
     * @return A {@link CellFormatResult} object containing the results of
     *         applying the format to the value.
     */
    public CellFormatResult apply(Object value) {
        boolean applies = applies(value);
        String text;
        Color textColor;
        if (applies) {
            text = format.format(value);
            textColor = color;
        } else {
            text = format.simpleFormat(value);
            textColor = null;
        }
        return new CellFormatResult(applies, text, textColor);
    }

    /**
     * Apply this format part to the given value, applying the result to the
     * given label.
     *
     * @param label The label
     * @param value The value to apply this format part to.
     *
     * @return {@code true} if the
     */
    public CellFormatResult apply(JLabel label, Object value) {
        CellFormatResult result = apply(value);
        label.setText(result.text);
        if (result.textColor != null) {
            label.setForeground(result.textColor);
        }
        return result;
    }

    /**
     * Returns the VCellFormatType object implied by the format specification for
     * the format part.
     *
     * @return The VCellFormatType.
     */
    VCellFormatType getCellFormatType() {
        return type;
    }

    /**
     * Returns {@code true} if this format part has a condition.
     *
     * @return {@code true} if this format part has a condition.
     */
    boolean hasCondition() {
        return condition != null;
    }

    public static StringBuffer parseFormat(String fdesc, VCellFormatType type,
            PartHandler partHandler) {

        // Quoting is very awkward.  In the Java classes, quoting is done
        // between ' chars, with '' meaning a single ' char. The problem is that
        // in Excel, it is legal to have two adjacent escaped strings.  For
        // example, consider the Excel format "\a\b#".  The naive (and easy)
        // translation into Java DecimalFormat is "'a''b'#".  For the number 17,
        // in Excel you would get "ab17", but in Java it would be "a'b17" -- the
        // '' is in the middle of the quoted string in Java.  So the trick we
        // use is this: When we encounter a ' char in the Excel format, we
        // output a \u0000 char into the string.  Now we know that any '' in the
        // output is the result of two adjacent escaped strings.  So after the
        // main loop, we have to do two passes: One to eliminate any ''
        // sequences, to make "'a''b'" become "'ab'", and another to replace any
        // \u0000 with '' to mean a quote char.  Oy.
        //
        // For formats that don't use "'" we don't do any of this
        Matcher m = SPECIFICATION_PAT.matcher(fdesc);
        StringBuffer fmt = new StringBuffer();
        while (m.find()) {
            String part = group(m, 0);
            if (part.length() > 0) {
                String repl = partHandler.handlePart(m, part, type, fmt);
                if (repl == null) {
                    switch (part.charAt(0)) {
                    case '\"':
                        repl = quoteSpecial(part.substring(1,
                                part.length() - 1), type);
                        break;
                    case '\\':
                        repl = quoteSpecial(part.substring(1), type);
                        break;
                    case '_':
                        repl = " ";
                        break;
                    case '*': //!! We don't do this for real, we just put in 3 of them
                        repl = expandChar(part);
                        break;
                    default:
                        repl = part;
                        break;
                    }
                }
                m.appendReplacement(fmt, Matcher.quoteReplacement(repl));
            }
        }
        m.appendTail(fmt);

        if (type.isSpecial('\'')) {
            // Now the next pass for quoted characters: Remove '' chars, making "'a''b'" into "'ab'"
            int pos = 0;
            while ((pos = fmt.indexOf("''", pos)) >= 0) {
                fmt.delete(pos, pos + 2);
                if (partHandler instanceof CellDateFormatter.DatePartHandler) {
                    CellDateFormatter.DatePartHandler datePartHandler = (CellDateFormatter.DatePartHandler) partHandler;
                    datePartHandler.updatePositions(pos, -2);
                }
            }

            // Now the final pass for quoted chars: Replace any \u0000 with ''
            pos = 0;
            while ((pos = fmt.indexOf("\u0000", pos)) >= 0) {
                fmt.replace(pos, pos + 1, "''");
                if (partHandler instanceof CellDateFormatter.DatePartHandler) {
                    CellDateFormatter.DatePartHandler datePartHandler = (CellDateFormatter.DatePartHandler) partHandler;
                    datePartHandler.updatePositions(pos, 1);
                }
            }
        }

        return fmt;
    }

    /**
     * Expands a character. This is only partly done, because we don't have the
     * correct info.  In Excel, this would be expanded to fill the rest of the
     * cell, but we don't know, in general, what the "rest of the cell" is.
     *
     * @param part The character to be repeated is the second character in this
     *             string.
     *
     * @return The character repeated three times.
     */
    static String expandChar(String part) {
        List<String> codePoints = new ArrayList<>();
        CodepointsUtil.iteratorFor(part).forEachRemaining(codePoints::add);
        if (codePoints.size() < 2) throw new IllegalArgumentException("Expected part string to have at least 2 chars");
        String ch = codePoints.get(1);
        return ch + ch + ch;
    }

    /**
     * Returns the string from the group, or {@code ""} if the group is
     * {@code null}.
     *
     * @param m The matcher.
     * @param g The group number.
     *
     * @return The group or {@code ""}.
     */
    public static String group(Matcher m, int g) {
        String str = m.group(g);
        return (str == null ? "" : str);
    }

    public String toString() {
        return format.format;
    }
}
