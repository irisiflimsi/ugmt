package miju.rpg.ugmt;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to help with parameters from a GET request. args[0] holds
 * class name.
 */
public class HttpQueryParams {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpQueryParams.class);

    /** argument hash. */
    private Map<String, String> params = new HashMap<>();

    /**
     * Constructor.
     * @param argv argument array to parse
     */
    public HttpQueryParams(final String[] argv) {
        for (int i = 1; i < argv.length; i += 2) {
            if (argv.length > i + 1) {
                params.put(argv[i], argv[i + 1]);
            }
        }
    }

    /** Standard getter. @return param */
    public String getEdit() {
        return getValue("edit", true);
    }

    /** Standard getter. @return param */
    public String getVal() {
        return getValue("val", true);
    }

    /** Standard getter. @return param */
    public String getKey() {
        return getValue("key", true);
    }

    /** Standard getter. @return param */
    public String getSave() {
        return getValue("save", true);
    }

    /** Standard getter. @return param */
    public String getView() {
        return getValue("view", true);
    }

    /** Standard getter. @return param */
    public String getTag() {
        return getValue("tag", true);
    }

    /** Standard getter. @return param */
    public String getTmpl() {
        return getValue("tmpl", true);
    }

    /** Standard getter. @return param */
    public String getId() {
        return getValue("id", true);
    }

    /** Standard getter. @return param */
    public String getSize() {
        return getValue("size", true);
    }

    /** Standard getter. @return param */
    public String getImg() {
        return getValue("img", true);
    }

    /** Standard getter. @return param */
    public String getSet() {
        return getValue("set", true);
    }

    /**
     * Get a value and potentially decode it. Rather primitive escaping.
     * @param param parameter to look for
     * @param dc decode it?
     * @return (decoded) parameter value or null
     * @throws UnsupportedEncodingException parameter can't be decoded
     */
    public String getValue(final String param, final boolean dc) {
        String ret = params.get(param);
        if (ret != null && dc) {
            try {
                ret = URLDecoder.decode(ret, StandardCharsets.UTF_8.name()).replaceAll("\"", "&");
            }
            catch (final UnsupportedEncodingException e) {
                LOGGER.error("", e);
            }
        }
        return ret;
    }
}
