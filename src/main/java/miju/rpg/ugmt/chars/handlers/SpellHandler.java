package miju.rpg.ugmt.chars.handlers;

import static miju.rpg.ugmt.XmlNames.Elements.SPELL;

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
public class SpellHandler {
    /** Logger. */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellHandler.class);

    /** Controller. */
    private CharProxy charProxy;

    /** Cache. */
    @SuppressWarnings("unused")
    private Map<String, String> cache = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor.
     * @param aCharProxy controller
     */
    public SpellHandler(final CharProxy aCharProxy) {
        charProxy = aCharProxy;
    }

    /**
     * Get all spells.
     * @return all spells
     */
    public List<String> getSpells() {
        return charProxy.getTags(SPELL);
    }

    /**
     * Get the attribute of a spell. Maybe from character or rules.
     * @param attr attribute to get
     * @param name name of spell
     * @return spell attribute
     */
    public String getSpellAttr(final String attr, final String name) {
        final String ch = charProxy.getAttrForNamedElem(SPELL, name, attr);
        if (ch != null & ch.length() != 0) {
            return ch;
        }
        final String ret = charProxy.getRules().getAttrForNamedTag(SPELL, name, attr);
        return ret;
    }
}
