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

import java.util.Formatter;
import java.util.Locale;

import org.apache.poi.util.LocaleUtil;

/**
 * A formatter for the default "General" cell format.
 */
public class VCellGeneralFormatter extends VCellFormatter {
    /** Creates a new general formatter. */
    public VCellGeneralFormatter() {
        this(LocaleUtil.getUserLocale());
    }
    /** Creates a new general formatter. */
    public VCellGeneralFormatter(Locale locale) {
        super(locale, "General");
    }

    /**
     * The general style is not quite the same as any other, or any combination
     * of others.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    @Override
    public void formatValue(StringBuffer toAppendTo, Object value) {
        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();
            if (val == 0) {
                toAppendTo.append('0');
                return;
            }

            String fmt;
            double exp = Math.log10(Math.abs(val));
            boolean stripZeros = true;
            if (exp > 10 || exp < -9)
                fmt = "%1.5E";
            else if ((long) val != val)
                fmt = "%1.9f";
            else {
                fmt = "%1.0f";
                stripZeros = false;
            }

            try (Formatter formatter = new Formatter(toAppendTo, locale)) {
                formatter.format(locale, fmt, value);
            }
            if (stripZeros) {
                // strip off trailing zeros
                int removeFrom;
                if (fmt.endsWith("E"))
                    removeFrom = toAppendTo.lastIndexOf("E") - 1;
                else
                    removeFrom = toAppendTo.length() - 1;
                while (toAppendTo.charAt(removeFrom) == '0') {
                    toAppendTo.deleteCharAt(removeFrom--);
                }
                if (toAppendTo.charAt(removeFrom) == '.') {
                    toAppendTo.deleteCharAt(removeFrom--);
                }
            }
        } else if (value instanceof Boolean) {
            toAppendTo.append(value.toString().toUpperCase(Locale.ROOT));
        } else {
            toAppendTo.append(value);
        }
    }

    /** Equivalent to {@link #formatValue(StringBuffer,Object)}. {@inheritDoc}. */
    @Override
    public void simpleValue(StringBuffer toAppendTo, Object value) {
        formatValue(toAppendTo, value);
    }
}
