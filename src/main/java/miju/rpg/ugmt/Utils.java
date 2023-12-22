package miju.rpg.ugmt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of mainly math utilities used when processing XML and GUI
 * elements.
 */
public final class Utils {
    /** Precision of doubles. */
    public static final double PRECISION = 0.0001;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /** Hide constructor. */
    private Utils() {
    }

    /**
     * Parse integer.
     * @param string string to parse
     * @return parsed integer
     */
    public static int parse(final String string) {
        if (string.startsWith("+")) {
            return parse(string.substring(1));
        }
        if (string.length() == 0) {
            return 0;
        }
        if (string.endsWith("%")) {
            return Integer.parseInt(string.substring(0, string.length() - 1));
        }
        if (string.endsWith(".0")) {
            return parse(string.substring(0, string.length() - 2));
        }
        return Integer.parseInt(string);
    }

    /**
     * Sign an integer.
     * @param value value to sign
     * @return a '+' was added to positive numbers
     */
    public static String toSignedString(final int value) {
        if (value >= 0) {
            return "+" + value;
        }
        else {
            return Integer.toString(value);
        }
    }

    /**
     * Return either the integer or the two decimal point double.
     * @param d double to parse
     * @return two decimal point or integer as string
     */
    public static String minimizeDouble(final double d) {
        String ds = Double.toString(d);
        ds = ds.substring(0, Math.min(ds.indexOf('.') + 3, ds.length()));
        if (ds.charAt(ds.length() - 1) == '0') {
            ds = ds.substring(0, ds.length() - 1);
        }
        if (ds.charAt(ds.length() - 1) == '0') {
            ds = ds.substring(0, ds.length() - 1);
        }
        if (ds.charAt(ds.length() - 1) == '.') {
            ds = ds.substring(0, ds.length() - 1);
        }
        return ds;
    }

    /**
     * Parse double.
     * @param string string to parse
     * @return parsed double
     */
    public static double parseDbl(final String string) {
        if (string.startsWith("+")) {
            return parseDbl(string.substring(1));
        }
        if (string.length() == 0) {
            return 0;
        }
        if (string.endsWith("%")) {
            return parseDbl(string.substring(0, string.length() - 1));
        }
        try {
            return Double.parseDouble(string.replace(',', '.'));
        }
        catch (final NumberFormatException e) {
            Matcher m = Pattern.compile("\\[([0-9+-]*).*\\]").matcher(string);
            if (m.find()) {
                LOGGER.debug("parseDbl for={}", m.group(1));
                return parseDbl(m.group(1));
            }
            LOGGER.warn("parseDbl default for={}", string);
            return 0;
        }
    }

}
