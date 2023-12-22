package miju.rpg.ugmt.chars.handlers;

import static miju.rpg.ugmt.XmlNames.Attributes.VALUE;
import static miju.rpg.ugmt.XmlNames.Elements.ATTRIBUTE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import miju.rpg.ugmt.chars.CharProxy;

/**
 * Delegate for certain character tags/entities.
 */
public class AttributeHandler {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeHandler.class);

    /** Controller. */
    private CharProxy charProxy;

    /** Cache. */
    private Map<String, String> cache = Collections.synchronizedMap(new HashMap<>());

    /**
     * Contructor.
     * @param aCharProxy controller
     */
    public AttributeHandler(final CharProxy aCharProxy) {
        charProxy = aCharProxy;
    }

    /**
     * Get all (potential) attributes.
     * @return all attributes
     */
    public List<String> getAttributes() {
        final List<String> ret = charProxy.getTags(ATTRIBUTE);
        ret.addAll(charProxy.getRules().getAttributes());
        return ret;
    }

    /**
     * Get the attribute.
     * @param attrName name of attribute
     * @return value of attribute
     */
    public String getAttribute(final String attrName) {
        String ret = cache.get(attrName);
        if (ret == null) {
            ret = charProxy.getAttrForNamedElem(ATTRIBUTE, attrName, VALUE);
            if (ret == null || ret.length() == 0) {
                ret = charProxy.calcAttribute(attrName);
            }
            LOGGER.debug("attribute: attrName={}, ret={}", attrName, ret);
            cache.put(attrName, ret);
        }
        return ret;
    }
}
