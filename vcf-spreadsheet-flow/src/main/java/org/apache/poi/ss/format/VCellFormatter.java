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

import java.util.Locale;

import org.apache.poi.util.LocaleUtil;

/**
 * This is the abstract supertype for the various cell formatters.
 */
public abstract class VCellFormatter {
    /** The original specified format. */
    protected final String format;
    protected final Locale locale;

    /**
     * Creates a new formatter object, storing the format in {@link #format}.
     *
     * @param format The format.
     */
    public VCellFormatter(String format) {
        this(LocaleUtil.getUserLocale(), format);
    }

    /**
     * Creates a new formatter object, storing the format in {@link #format}.
     *
     * @param locale The locale.
     * @param format The format.
     */
    public VCellFormatter(Locale locale, String format) {
        this.locale = locale;
        this.format = format;
    }

    /**
     * Format a value according the format string.
     * <p>
     * NOTE: this method must be thread safe!  In particular, if it uses a
     * Format instance that is not thread safe, i.e. DateFormat, this method
     * must be synchronized, either on the method, if the format is a final
     * property, or on the format instance itself.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public abstract void formatValue(StringBuffer toAppendTo, Object value);

    /**
     * Format a value according to the type, in the most basic way.
     * <p>
     * NOTE: this method must be thread safe!  In particular, if it uses a
     * Format instance that is not thread safe, i.e. DateFormat, this method
     * must be synchronized, either on the method, if the format is a final
     * property, or on the format instance itself.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public abstract void simpleValue(StringBuffer toAppendTo, Object value);

    /**
     * Formats the value, returning the resulting string.
     *
     * @param value The value to format.
     *
     * @return The value, formatted.
     */
    public String format(Object value) {
        StringBuffer sb = new StringBuffer();
        formatValue(sb, value);
        return sb.toString();
    }

    /**
     * Formats the value in the most basic way, returning the resulting string.
     *
     * @param value The value to format.
     *
     * @return The value, formatted.
     */
    public String simpleFormat(Object value) {
        StringBuffer sb = new StringBuffer();
        simpleValue(sb, value);
        return sb.toString();
    }

    /**
     * Returns the input string, surrounded by quotes.
     *
     * @param str The string to quote.
     *
     * @return The input string, surrounded by quotes.
     */
    static String quote(String str) {
        return '"' + str + '"';
    }
}
