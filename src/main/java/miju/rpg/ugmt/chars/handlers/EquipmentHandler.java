package miju.rpg.ugmt.chars.handlers;

import static miju.rpg.ugmt.XmlNames.Attributes.NAME;
import static miju.rpg.ugmt.XmlNames.Attributes.MATERIAL;
import static miju.rpg.ugmt.XmlNames.Elements.EQUIP;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import miju.rpg.ugmt.chars.CharProxy;
import miju.rpg.ugmt.chars.NamedElement;

/**
 * Delegate for certain character tags/entities.
 */
public class EquipmentHandler {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentHandler.class);

    /** equipment attributes to replace. */
    private static final List<String> ATTRIBUTES = Arrays.asList("Rdamage", "Renc",
            "Rmod", "Rticks", "Rvalue", "Rwgs", "class", "clip", "crit", "enc",
            "location", "Rmal", "munition", "pen", "qualities", "range",
            "reload", "rof", "skill", "special", "wtype");

    /** Controller. */
    private CharProxy charProxy;

    /** Cache. */
    private Map<String, String> cache = Collections.synchronizedMap(new HashMap<>());

    /**
     * Contructor.
     * @param aCharProxy controller
     */

    public EquipmentHandler(final CharProxy aCharProxy) {
        charProxy = aCharProxy;
    }

    /**
     * Replace all equipment in the character sheet. This depends heavily on the
     * rules, and usually calculations have to be made.
     * @param pre prefix to be looked for in the template string
     * @param preOut String to be replaced (in)
     * @param item equipment item as DOM
     * @return transformed text
     */
    public String replaceAllEquipment(final String pre, final String preOut, final NamedElement item) {
        LOGGER.debug("pre={}", pre);
        String out = preOut;
        out = out.replaceAll(pre + "C", replaceEquipmentC(item));
        out = out.replaceAll(pre + "N", replaceEquipmentN(item));
        out = out.replaceAll(pre + "M", replaceEquipmentM(item));
        for (String attr : ATTRIBUTES) {
            if (attr.charAt(0) == 'R') { // Match a rule
                out = out.replaceAll(pre + attr, getAttrFromEquipment(item, attr.substring(1)));
            }
            else { // Take verbatim
                out = out.replaceAll(pre + attr, charProxy.getRules().getEquipmentAttribute(item, attr));
            }
        }
        return out;
    }

    /**
     * Get modifier value.
     * @param item item to get modifier for
     * @param attr attribute to get
     * @return modifier for item
     */
    public String getAttrFromEquipment(final NamedElement item, final String attr) {
        final String name = item.getAttribute(NAME);
        LOGGER.debug("name={}, attr={}", name, attr);
        String ret = cache.get(name + "@" + attr);
        if (ret == null) {
            ret = charProxy.replaceEquipment(item, attr, name);
            LOGGER.debug("ret={}", ret);
            cache.put(name + "@" + attr, ret);
        }
        return ret;
    }

    /**
     * Get container.
     * @param item item to get container for
     * @return container for item
     */
    private String replaceEquipmentC(final NamedElement item) {
        if (item.getParentNode().getNodeName().equals(EQUIP)) {
            return ((Element) item.getParentNode()).getAttribute(NAME);
        }
        return "";
    }

    /**
     * Get name value.
     * @param item item to get name for
     * @return name for item
     */
    private String replaceEquipmentN(final NamedElement item) {
        return item.getAttribute(NAME).replace("&", "&amp;");
    }

    /**
     * Get material value.
     * @param item item to get name for
     * @return name for item
     */
    private String replaceEquipmentM(final NamedElement item) {
        return item.getAttribute(MATERIAL);
    }
}
