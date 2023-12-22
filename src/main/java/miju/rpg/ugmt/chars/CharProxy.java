package miju.rpg.ugmt.chars;

import static miju.rpg.ugmt.XmlNames.Attributes.FILE;
import static miju.rpg.ugmt.XmlNames.Attributes.ID;
import static miju.rpg.ugmt.XmlNames.Attributes.NAME;
import static miju.rpg.ugmt.XmlNames.Attributes.VALUE;
import static miju.rpg.ugmt.XmlNames.Attributes.TYPE;
import static miju.rpg.ugmt.XmlNames.Elements.EQUIP;
import static miju.rpg.ugmt.Utils.parse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import miju.rpg.ugmt.Utils;
import miju.rpg.ugmt.XmlNames.Elements;
import miju.rpg.ugmt.XmlPrinter;
import miju.rpg.ugmt.XmlStreamsUtil;
import miju.rpg.ugmt.chars.handlers.AttributeHandler;
import miju.rpg.ugmt.chars.handlers.EquipmentHandler;
import miju.rpg.ugmt.chars.handlers.SkillHandler;
import miju.rpg.ugmt.chars.handlers.SpellHandler;

/**
 * Base class for all char proxies. Must be instantiated, because all rules need their peculiarities.
 */
public final class CharProxy {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CharProxy.class);

    /** Var name in rules. */
    private static final String VARNAME = "\\$name";

    /** Helper string constant. */
    private static final String IMAGE = "image";

    /** Helper string constant. */
    private static final String NOTE = "note";

    /** Helper string constant. */
    private static final String VOID_PNG = "void.png";

    /** attributes, skills, ... */
    private NodeList charNodes;

    /** Rules. */
    private Rules rules;

    /** Id. */
    private String charId;

    /** Rules construction helper. */
    private XPathProvider xPathProvider;

    /** Delegate handling of certain character entities. */
    private AttributeHandler attributeHandler;

    /** Delegate handling of certain character entities. */
    private SkillHandler skillHandler;

    /** Delegate handling of certain character entities. */
    private SpellHandler spellHandler;

    /** Delegate handling of certain character entities. */
    private EquipmentHandler equipmentHandler;

    /**
     * Constructor. Get the basics.
     * @param elem element to construct for
     * @throws IOException on error
     */
    private CharProxy(final Element elem) throws IOException, XPathFactoryConfigurationException {
        charNodes = elem.getChildNodes();
        xPathProvider = new XPathProvider(this);

        attributeHandler = new AttributeHandler(this);
        skillHandler = new SkillHandler(this);
        equipmentHandler = new EquipmentHandler(this);
        spellHandler = new SpellHandler(this);
        final NamedNodeMap attr = elem.getAttributes();
        charId = attr.getNamedItem(ID).getNodeValue();
        rules = Rules.getRule(elem.getAttribute("rules"));
    }

    /**
     * Get a character proxy.
     * @param charNode XML element to proxy
     * @return proxy for character
     * @throws Exception on error
     */
    static CharProxy getCharacterProxy(final Element charNode) throws Exception {
        return new CharProxy(charNode);
    }

    /**
     * Get the Char Nodes.
     * @return the char nodes
     */
    public NodeList getCharNodes() {
        return charNodes;
    }

    /**
     * Get the XPath Provider.
     * @return the xpath provider
     */
    public XPathProvider getXPathProvider() {
        return xPathProvider;
    }

    /**
     * Get the rules.
     * @return the rules
     */
    public Rules getRules() {
        return rules;
    }

    /**
     * Get the char id.
     * @return char id
     */
    public String getId() {
        return charId;
    }

    /**
     * Get the char name.
     * @return char name
     */
    public String getName() {
        return getAttrForNamedElem(Elements.ATTRIBUTE, "Name", VALUE);
    }

    /**
     * Get all (potential) attributes.
     * @return all attributes
     */
    public List<String> getAttributes() {
        return attributeHandler.getAttributes();
    }

    /**
     * Get the attribute.
     * @param name name of attribute
     * @return value of attribute
     */
    String getAttribute(final String name) {
        return attributeHandler.getAttribute(name);
    }

    /**
     * Get all skills.
     * @return all skills
     */
    public List<String> getSkills() {
        return skillHandler.getSkills();
    }

    /**
     * Get all skills.
     * @return all skills
     */
    public List<Element> getSkillElements() {
        return skillHandler.getSkillElements();
    }

    /**
     * Get all spells.
     * @return all spells
     */
    public List<String> getSpells() {
        return spellHandler.getSpells();
    }

    /**
     * Get the attribute "attr" from the "skill".
     * @param skill skill to look in
     * @param attr attribute to look for
     * @return value of attr
     */
    String getSkillAttribute(final String skill, final String attr) {
        return skillHandler.getSkillAttribute(skill, attr);
    }

    /**
     * Sort rule equipment.
     * @return sorted equipment elements
     */
    public SortedSet<NamedElement> getEquipment() {
        return getSortedTags(EQUIP);
    }

    /**
     * Get credits.
     * @return value of credits
     */
    public String getCredits() {
        return getTypedNote("credits");
    }

    /**
     * Get deity
     * @return value of deity
     */
    public String getDeity() {
        return getAttribute("Deity");
    }

    /**
     * Get deity
     * @return value of deity
     */
    public String getPsyche() {
        return getAttribute("Psyche");
    }

    /**
     * Get deity
     * @return value of deity
     */
    public String getMedical() {
        return getAttribute("Medical");
    }

    /**
     * Get background.
     * @return value of background
     */
    public String getBackground() {
        return getTypedNote("background");
    }

    /**
     * Get short note at index.
     * @param idx index
     * @return short note at index
     */
    String getShortNote(final int idx) {
        return getTypedNote("short-" + idx);
    }

    /**
     * Get note of type.
     * @param type type of note
     * @return all children nodes as string
     */
    private String getTypedNote(final String type) {
        return getFirstMappedTagWithCondition(NOTE, e -> XmlPrinter.printNodeChildren(e),
                e -> e.getAttribute(TYPE).equals(type), "");
    }

    /**
     * Get image of type marker.
     * @return marker image file name
     */
    public String getMarker() {
        return getFirstMappedTagWithCondition(IMAGE, e -> e.getAttribute(FILE),
                e -> e.getAttribute(TYPE).equals("marker"), VOID_PNG);
    }

    /**
     * Get the attribute value for a named tag (element).
     * @param tag tag to look for
     * @param name name of element
     * @param attr attribute to look for
     * @return value of attribute
     */
    public String getAttrForNamedElem(final String tag, final String name, final String attr) {
        return getFirstMappedTagWithCondition(tag, e -> e.getAttribute(attr), e -> e.getAttribute(NAME).equals(name), "");
    }

    /**
     * Find the first tag in the list which matches condition and return what's mapped for it.
     * @param tag tag to look for
     * @param map map after condition applied
     * @param condition condition to check before mapping
     * @param defawlt default answer
     * @param <T> type of map result
     * @return first mapped element found
     */
    private <T> T getFirstMappedTagWithCondition(final String tag, final Function<Element, T> map, final Predicate<Element> condition, final T defawlt) {
        return XmlStreamsUtil.elementStream(charNodes).filter(e -> e.getNodeName().equals(tag)).filter(e -> condition.test(e))
                .map(map).findFirst().orElse(defawlt);
    }

    /**
     * Get all tags' name.
     * @param tag tag-name of element
     * @return all names of all found elements
     */
    public List<String> getTags(final String tag) {
        return XmlStreamsUtil.getMappedTags(charNodes, tag, e -> e.getAttribute(NAME));
    }

    /**
     * Get the portrait or heraldry image.
     * @return file name of image
     */
    public String getPortrait() {
        String ret = null;
        for (String type : Arrays.asList("portrait", "heraldry")) {
            ret = getFirstMappedTagWithCondition(IMAGE, e -> e.getAttribute(FILE),
                    e -> e.getAttribute(TYPE).equals(type), VOID_PNG);
            if (ret != null) {
                return ret;
            }
        }
        return VOID_PNG;
    }

    /**
     * Sort elements of type tag.
     * @param tag element type
     * @return sorted set
     */
    private SortedSet<NamedElement> getSortedTags(final String tag) {
        final SortedSet<NamedElement> ret = new TreeSet<NamedElement>();
        try {
            final NodeList list = (NodeList) xPathProvider.compile("//char[@id='" + charId + "']//" + tag)
                    .evaluate(charNodes, XPathConstants.NODESET);
            for (int i = 0; i < list.getLength(); i++) {
                ret.add(new NamedElement((Element) list.item(i)));
            }
        }
        catch (final Exception e) {
            // Ignore
        }
        return ret;
    }

    /**
     * Calculate the attribute according to rules.
     * @param name name of attribute
     * @return value of attribute
     */
    public String calcAttribute(final String name) {
        try {
            final String rule = getRules().getAttrForNamedTag("attribute", name, "rule");
            LOGGER.debug("attribute: name={}, rule={}", name, rule);
            if (rule != null && rule.length() != 0) {
                getXPathProvider().setContextName(name);
                return (String) getXPathProvider().compile(rule)
                        .evaluate(getCharNodes(), XPathConstants.STRING);
            }
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
        return "";
    }

    /**
     * Replace all skills in the character sheet. This depends heavily on the
     * rules, and usually calculations have to be made.
     * @param pre prefix that the trafo class looks for
     * @param preOut the string containing the replaceable passages
     * @param skill normalized skill name
     * @param section section the skill belongs to
     * @return replaced string
     */
    String replaceAllSkills(final String pre, final String preOut, final String skill, final String section) {
        return skillHandler.replaceAllSkills(pre, preOut, skill, section);
    }

    /**
     * Replace all equipment in the character sheet. This depends heavily on the
     * rules, and usually calculations have to be made.
     * @param pre prefix to be looked for in the template string
     * @param preOut String to be replaced (in)
     * @param item equipment item as DOM
     * @return transformed text
     */
    String replaceAllEquipment(final String pre, final String preOut, final NamedElement item) {
        return equipmentHandler.replaceAllEquipment(pre, preOut, item);
    }

    /**
     * Get class bonus for skill.
     * @param name skill name
     * @return class bonus
     */
    public String getClassSkill(final String name) {
        String ret = "";
        try {
            final NodeList list = (NodeList) getXPathProvider().compile(".//attribute[starts-with(@name, 'Class')]/@value")
                    .evaluate(getCharNodes(), XPathConstants.NODESET);
            LOGGER.debug("class: list.length={}", list.getLength());
            for (int i = 0; i < list.getLength(); i++) {
                final List<String> subnames = Arrays.asList(name.split("/"));
                Collections.reverse(subnames);
                for (String subname : subnames) {
                    final String rule = getRules().getClassAttr(list.item(i).getNodeValue(), VALUE)
                            .replaceAll(VARNAME, subname);
                    LOGGER.debug("class: subname={}, rule={}", subname, rule);
                    if (rule != null && rule.length() != 0) {
                        getXPathProvider().setContextName(name);
                        final String next = (String) getXPathProvider()
                                .compile(rule).evaluate(getRules().getRoot(), XPathConstants.STRING);
                        LOGGER.debug("class: next={}", next);
                        ret = Utils.toSignedString(Math.max(parse(ret), parse(next)));
                    }
                }
            }
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
        LOGGER.debug("class: ret={}", ret);
        return ret;
    }

    /**
     * Get the attribute of a spell. Maybe from character or rules.
     * @param attr attribute to get
     * @param name name of spell
     * @return spell attribute
     */
    public String getSpellAttr(final String attr, final String name) {
        return spellHandler.getSpellAttr(attr, name);
    }

    /**
     * Get modifier value.
     * @param item item to get modifier for
     * @param attr attribute to get
     * @param name name of equipment item
     * @return modifier for item
     */
    public String replaceEquipment(final NamedElement item, final String attr, final String name) {
        final String raw = Rules.getRawName(name);
        String mod = "";
        final Matcher m = Pattern.compile((Rules.REGEX_ANY + Rules.REGEX_MOD + Rules.REGEX_ANY)).matcher(name);
        if (m.find() && m.groupCount() == 1) {
            mod = m.group(1);
        }
        LOGGER.debug("raw={}, mod={}", raw, mod);
        return doIt("gear", name, attr, (Element) getRules().getEquipmentByName(raw), getRules().getEquipmentAttribute(item, attr));
    }

    /**
     * Get the attributes section from rules skill (not character).
     * @param skill skill to look for
     * @param attr attribute to look
     * @param subname sub skill name
     * @return evaluated rule
     * @throws XPathExpressionException on error
     */
    public String getRulesSkillAttribute(final String skill, final String attr, final String subname) throws XPathExpressionException {
        return doIt("section", skill, attr, (Element) getRules().getSkillByName(subname), "");
    }

    /**
     * Evaluate a rule section.
     * @param elemName elem name to find
     * @param ctxName name to store in context for rule evaluation
     * @param attr attribute to look for rule
     * @param elemStart starting element
     * @param ruleStart initial rule
     * @return evaluated rule
     */
    private String doIt(final String elemName, final String ctxName, final String attr, final Element elemStart, final String ruleStart) {
        LOGGER.debug("elemName={}, ctxName={}, attr={}, ruleStart={}", elemName, ctxName, attr, ruleStart);
        String ret = "";
        Element elem = elemStart;
        String rule = ruleStart;
        try {
            while (elem != null && rule.length() == 0) {
                if (elem.getNodeName().equals(elemName)) {
                    rule = getXPathProvider().compile("./modifier[@name='" + attr + "']/@value").evaluate(elem);
                    elem = null;
                }
                else {
                    rule = elem.getAttribute(attr);
                    if (elem.getParentNode() instanceof Element) {
                        elem = (Element) elem.getParentNode();
                    }
                    else {
                        elem = null;
                    }
                }
            }
            LOGGER.debug("rule={}", rule);
            if (rule != null) {
                ret = rule; // Assume pre-evaluation, when rule is short
                if (rule.length() > 3) {
                    getXPathProvider().setContextName(ctxName);
                    ret = (String) getXPathProvider().compile(rule).evaluate(getCharNodes(), XPathConstants.STRING);
                }
            }
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
        return ret;
    }
}
