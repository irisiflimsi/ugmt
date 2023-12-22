package miju.rpg.ugmt.chars;

import static miju.rpg.ugmt.Utils.minimizeDouble;
import static miju.rpg.ugmt.Utils.parseDbl;
import static miju.rpg.ugmt.XmlNames.Attributes.NAME;
import static miju.rpg.ugmt.XmlNames.Attributes.VALUE;
import static miju.rpg.ugmt.XmlNames.Elements.ATTRIBUTE;
import static miju.rpg.ugmt.XmlNames.Elements.EQUIP;
import static miju.rpg.ugmt.XmlNames.Elements.SKILL;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import miju.rpg.ugmt.AbstractXmlProvider;
import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.Monitor;
import miju.rpg.ugmt.Utils;
import miju.rpg.ugmt.XmlStreamsUtil;

/**
 * Rules base class. Contains the document but also caches sections.
 */
public final class Rules extends AbstractXmlProvider {
    /** Equipment modifier. */
    public static final String REGEX_MOD = " \\[(.*)\\]";

    /** Equipment number. */
    public static final String REGEX_NUM = " \\((\\d+)\\)";

    /** Regex helper. */
    public static final String REGEX_ANY = ".*";

    /** Helper string constant. */
    static final String CLASS = "class";

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Rules.class);

    /** Helper string constant. */
    private static final String KEY0 = "key0";

    /** Helper string constant. */
    private static final String KEY1 = "key1";

    /** Helper string constant. */
    private static final String SECTION = "section";

    /** Rules cache. */
    private static final Map<String, Rules> RULES = new HashMap<String, Rules>();

    /** Section cache. */
    private final Map<String, String> sections = new Hashtable<String, String>();

    /**
     * Constructor.
     * @param absPath file for this rule set
     * @throws ParserConfigurationException on error
     * @throws Exception on error
     */
    private Rules(final Path absPath) throws Exception {
        final List<Path> ret = new ArrayList<>();
        final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(absPath);
        for (Path path : directoryStream) {
            final Path pathName = path.getFileName();
            if (pathName != null && pathName.toString().equals("rules.xml")) {
                ret.add(path);
            }
        }
        if (ret.size() == 0) {
            throw new IndexOutOfBoundsException("No rules found in: " + absPath.toString());
        }
        final Path watchAbsPath = ret.get(0);
        setRoot(parse(watchAbsPath));
        Monitor.monitor(this, watchAbsPath.getParent());

        // Fill the section cache
        XmlStreamsUtil.elementStream(getRoot().getDocumentElement().getElementsByTagName(SECTION)).forEach(e -> fillSections("", e, e.getAttribute(NAME)));
    }

    /**
     * Get the multiplied value, if a factor and a numeric value are available.
     * @param fullName full name, potentially with factor
     * @param rawValue value, potentially a number
     * @return multiplied or raw value
     */
    public static String getMultipliedValue(String fullName, String rawValue) {
        final String rawName = getRawName(fullName);
        final String regex = rawName + Rules.REGEX_ANY + " \\((\\d+)\\)" + Rules.REGEX_ANY;
        final Matcher m = Pattern.compile(regex).matcher(fullName);
        LOGGER.debug("fullName={}, rawName={}, regex={}, rawValue={}", fullName, rawName, regex, rawValue);
        if (m.find() && m.groupCount() == 1 && rawValue.matches("-?\\d*(\\.\\d+)?")) {
            return minimizeDouble(parseDbl(m.group(1)) * parseDbl(rawValue));
        }
        return rawValue;
    }

    /**
     * Get the raw name of an item.
     * @param name modfied and numbered name of an item
     * @return raw name
     */
    public static String getRawName(final String name) {
        return name.replaceAll(Rules.REGEX_NUM, "").replaceAll(Rules.REGEX_MOD, "");
    }

    /**
     * Create the rules.
     * @param version rule name
     * @return get (all) rules
     * @throws IOException on error
     */
    static synchronized Rules getRule(final String version) throws IOException {
        Rules rules = RULES.get(version);
        if (rules == null) {
            for (Path absPath : getRulesFiles()) {
                final Path fileName = absPath.getFileName();
                if (fileName != null && fileName.toString().equals(version)) {
                    try {
                        rules = new Rules(absPath);
                        RULES.put(version, rules);
                        LOGGER.info("Got char rules version={}", version);
                    }
                    catch (final Exception e) {
                        LOGGER.error("", e);
                    }
                }
            }
        }
        return rules;
    }

    @Override
    public synchronized void load(final Path absPath) throws SAXException, IOException {
        LOGGER.info("Loading path={}", absPath);
        setRoot(parse(absPath));
    }

    /**
     * Getter.
     * @param skill skill to get section for
     * @return section of skill
     */
    String getSection(final String skill) {
        return sections.get(skill);
    }

    /**
     * Get the row of a table that is between keyL and keyH.
     * @param table table name
     * @param key0 first key to match
     * @param key1 second key to match
     * @return value of that row
     */
    String getRowValueInTableForKey(final String table, final double key0, final String key1) {
        return reduceSubtags(tagsNamedValue("table", NAME, table), "row")
                .filter(new DoubleKeyMatchPredicate(key0, key1))
                .map(e -> e.getAttribute(VALUE)).findFirst().orElse("");
    }

    /**
     * Get equipment with name.
     * @param name name of item
     * @return element
     */
    Element getEquipmentByName(final String name) {
        return tagsNamedValue(EQUIP, NAME, name).findAny().orElse(null);
    }

    /**
     * Get all tags with name and get the first attr.
     * @param tag tag to look for
     * @param name name to verify
     * @param attr attribute to get
     * @return first find
     */
    public String getAttrForNamedTag(final String tag, final String name, final String attr) {
        return tagsNamedValue(tag, NAME, name).map(e -> e.getAttribute(attr)).findAny().orElse("");
    }

    /**
     * Get all (rule) attributes.
     * @return all attributes
     */
    public List<String> getAttributes() {
        return tags(ATTRIBUTE).flatMap(Rules::getElementMultTimesValue).collect(Collectors.toList());
    }

    /**
     * Get all talents relevant for skill. (Reverse lookup.)
     * @param skill skill to scan
     * @return list of talents for skill
     */
    public List<String> getSkillTalents(final String skill) {
        return getSkillsInSection("Talents").filter(e -> getElemRelatedContains(e, skill))
                .map(Rules::getAttrName).collect(Collectors.toList());
    }

    /**
     * Get attribute for a class.
     * @param clas class to get attribute from
     * @param attr attribute to get
     * @return value of attribute
     */
    public String getClassAttr(final String clas, final String attr) {
        return getAttrForNamedTag(CLASS, clas, attr);
    }

    /**
     * Get Skill element by name.
     * @param name skill name
     * @return skill element
     */
    public Element getSkillByName(final String name) {
        return tags(SKILL).filter(e -> e.getAttribute(NAME).equals(name)).findAny().orElse(null);
    }

    /**
     * Get the attributes for an equipment item. In general some attributes are
     * derived, such that a rule specific normalization on the item's name needs
     * to take place. Default to not find anything.
     * @param item item to look for in the equipment list
     * @param attr attribute value to return
     * @return attribute value
     */
    public String getEquipmentAttribute(final NamedElement item, final String attr) {
        final String itemRawName = getRawName(item.getAttribute(NAME));
        LOGGER.debug("name={}, itemRawName={}, attr={}", item.getAttribute(NAME), itemRawName, attr);
        final Element itemRaw = getEquipmentByName(itemRawName);
        String ret = "";
        if (itemRaw != null) {
            final String attrValue = itemRaw.getAttribute(attr);
            ret = getMultipliedValue(item.getAttribute(NAME), attrValue);
        }
        LOGGER.debug("ret={}", ret);
        return ret;
    }

    /**
     * Return all rule directories.
     * @return all rule files (absolute paths)
     * @throws IOException on error
     */
    private static List<Path> getRulesFiles() throws IOException {
        final Path absPath = Data.ROOT_ABS_PATH.resolve("chars");
        final List<Path> ret = new ArrayList<>();
        final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(absPath);
        for (Path path : directoryStream) {
            if (Files.isDirectory(path)) {
                ret.add(path);
            }
        }
        return ret;
    }
    /**
     * Recursively fills in the section cache. The name of the sections is
     * normalized by concatenating the hierarchy names, separated by "/".
     * @param prefix Prefix from parent sections
     * @param elem this section's node
     * @param sectionName this section's name
     */
    private void fillSections(final String prefix, final Element elem, final String sectionName) {
        final NodeList list = elem.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (!(list.item(i) instanceof Element)) {
                continue;
            }
            final String name = ((Element) list.item(i)).getAttribute(NAME);
            sections.put(prefix + name, sectionName);
            fillSections(prefix + name + "/", (Element) list.item(i), sectionName);
        }
    }

    /**
     * Get attribute "name".
     * @param e element
     * @return attribute
     */
    private static String getAttrName(final Node e) {
        return ((Element) e).getAttribute(NAME);
    }

    /**
     * Get all tags.
     * @param tag tag to look for
     * @return all tag-elements found
     */
    private Stream<Element> tags(final String tag) {
        return XmlStreamsUtil.elementStream(getRoot().getElementsByTagName(tag));
    }

    /**
     * Get all tags.
     * @param elem root element
     * @param tag tag to look for
     * @return all tag-elements found
     */
    private Stream<Element> subtags(final Element elem, final String tag) {
        return XmlStreamsUtil.elementStream(elem.getElementsByTagName(tag));
    }

    /**
     * Get all tags whose attribute name is value.
     * @param tag tag to look for
     * @param name name of attribute
     * @param value value to match in attribute name
     * @return all elements found
     */
    private Stream<Element> tagsNamedValue(final String tag, final String name, final String value) {
        return tags(tag).filter(e -> e.getAttribute(name).equals(value));
    }

    /**
     * Get all subtag elements of streamed elements.
     * @param stream stream to reduce
     * @param subtag subtag elements to get
     * @return subtag element stream
     */
    private Stream<Element> reduceSubtags(final Stream<Element> stream, final String subtag) {
        return stream.flatMap(e -> subtags(e, subtag));
    }

    /**
     * Get all skills in section.
     * @param section section to query
     * @return all skills in section
     */
    private Stream<Element> getSkillsInSection(final String section) {
        return tagsNamedValue(SECTION, NAME, section).map(e -> e.getElementsByTagName(SKILL))
                .flatMap(XmlStreamsUtil::elementStream);
    }

    /**
     * Get the mult attribute of an element and multiple it by the name.
     * @param e element to multiply
     * @return multiplied list of names
     */
    private static Stream<String> getElementMultTimesValue(final Element e) {
        return Arrays.asList(e.getAttribute("mult").split(",")).stream().map(s -> e.getAttribute(NAME) + s);
    }

    /**
     * Test whether the attr attribute of e contains test.
     * @param e element to check
     * @param test test to check for containment
     * @return whether test succeeded
     */
    private boolean getElemRelatedContains(final Element e, final String test) {
        return Arrays.asList(e.getAttribute("related").split(",")).contains(test);
    }

    /**
     * Predicate for double key matches in table.
     */
    private static final class DoubleKeyMatchPredicate
            implements Predicate<Element> {
        private final String key1;
        private final double key0;

        /**
         * Constructor.
         * @param aKey0 first key
         * @param aKey1 second key
         */
        private DoubleKeyMatchPredicate(final double aKey0, final String aKey1) {
            this.key0 = aKey0;
            this.key1 = aKey1;
        }

        @Override
        public boolean test(final Element e) {
            final double low;
            final double high;
            if (e.getAttribute(KEY0).contains(" ")) {
                final String[] limits = e.getAttribute(KEY0).split(" ");
                low = Utils.parse(limits[0]);
                high = Utils.parse(limits[1]);
            }
            else {
                low = Utils.parse(e.getAttribute(KEY0));
                high = low;
            }
            boolean key1Match = true;
            if (e.getAttribute(KEY1) != null && e.getAttribute(KEY1).length() > 0) {
                key1Match = e.getAttribute(KEY1).equals(key1);
            }

            final boolean ret = (low <= key0 && high >= key0 && key1Match);
            LOGGER.debug("low={}, key0={}, high={} key1.att={} key1={} key1Match={} ret={}",
                    low, key0, high, e.getAttribute(KEY1), key1, key1Match, ret);
            return ret;
        }
    }
}
