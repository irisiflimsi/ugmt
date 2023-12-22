package miju.rpg.ugmt.combat;

import static miju.rpg.ugmt.Utils.parse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import miju.rpg.ugmt.AbstractMain;
import miju.rpg.ugmt.AbstractXmlProvider;
import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.HttpQueryParams;

/**
 * Main launch class. TODO This is Splittermond only.
 */
public class Main extends AbstractMain { // NO_UCD (unused code)
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /** XML attribute constant. */
    private static final String DAM = "damage";

    /** XML attribute constant. */
    private static final String WGS = "wgs";

    /** XML attribute constant. */
    private static final String VAL = "wert";

    /** Count for ids. */
    private static AtomicInteger count = new AtomicInteger();

    /** XPath helper. */
    private XPath xpath = XPathFactory.newInstance().newXPath();

    @Override
    public Object getContent(final HttpQueryParams args, final boolean gm) throws Exception {
        final String id = args.getId();
        if (id != null) {
            count.incrementAndGet();
            return getCharXML(id, gm);
        }
        return super.getContent(args, gm);
    }

    /**
     * Provides an XML string from a file modified for a character. Empty if no
     * file found.
     * @param id id of character
     * @param gm permit cascade
     * @return XML string
     * @throws Exception on error
     */
    private String getCharXML(final String id, final boolean gm) throws Exception {
        // Get char
        final Element charNode = getData().getElementById(id);
        if (charNode == null || !(gm || permit(charNode))) {
            return "";
        }
        final String rules = charNode.getAttribute("rules");
        LOGGER.debug("id={}, rules={}", id, rules);

        // Check rules specific and default template
        final Path absPath = Data.ROOT_ABS_PATH.resolve(Paths.get("combat", rules, "line.xml"));
        LOGGER.debug("absPath={}", absPath);
        if (!Files.exists(absPath)) {
            return "";
        }

        final Document doc = AbstractXmlProvider.parse(Data.ROOT_ABS_PATH.resolve(Paths.get("chars", rules, "rules.xml")));
        LOGGER.debug("doc={}", doc);
        return transform(id, new String(Files.readAllBytes(absPath)), doc);
    }

    /**
     * transform the template for the character/combatant.
     * @param id character id
     * @param template tempkate to fill
     * @param rules rule document
     * @return filled template (html node)
     * @throws XPathExpressionException on error
     */
    private String transform(final String id, final String template, final Document rules) throws XPathExpressionException {
        LOGGER.debug("id={}", id);
        String out = template;
        final Element ch = getData().getElementById(id);
        final String name = (String) xpath.compile("@name").evaluate(ch, XPathConstants.STRING);
        out = out.replaceAll("\\$Name", name + "-" + count);

        final String vtd = getVTD(rules, ch);
        out = out.replaceAll("\\$VTD", vtd);

        LOGGER.debug("name={}, vtd={}", name, vtd);
        final NodeList list = (NodeList) xpath.compile(".//equipment[@type='Waffe']")
                .evaluate(ch, XPathConstants.NODESET);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.getLength(); i++) {
            final Element citem = (Element) list.item(i);
            final String iname = citem.getAttribute("name");
            final Element ritem = (Element) xpath.compile(".//equipment[@name='" + iname + "']")
                    .evaluate(rules, XPathConstants.NODE);

            String iwgs = citem.getAttribute(WGS);
            String idam = citem.getAttribute(DAM);
            String ival = citem.getAttribute(VAL);
            LOGGER.debug("char: iwgs={}, idam={}, ival={}", iwgs, idam, ival);
            if (iwgs == null || iwgs.length() == 0) {
                iwgs = ritem.getAttribute(WGS);
            }
            if (idam == null || idam.length() == 0) {
                idam = ritem.getAttribute(DAM);
            }
            if (ival == null || ival.length() == 0) {
                ival = getSkill(ritem, ch);
            }
            LOGGER.debug("rules: iwgs={}, idam={}, ival={}", iwgs, idam, ival);
            sb.append("<option data-gsw='").append(iwgs)
                    .append("' data-dam='").append(idam)
                    .append("' value='").append(ival)
                    .append("'>").append(iname)
                    .append("</option>");
        }
        out = out.replaceAll("\\$ATYP", sb.toString());
        return out;
    }

    /**
     * Get VTD.
     * @param rules rules XML
     * @param ch character XML
     * @return VTD
     * @throws XPathExpressionException on error
     */
    private String getVTD(final Document rules, final Element ch) throws XPathExpressionException {
        final String base = (String) xpath.compile("attribute[@name='VTD']/@value")
                .evaluate(ch, XPathConstants.STRING);
        if (base != null && base.length() > 0) {
            return base;
        }
        final int bew = parse((String) xpath.compile("attribute[@name='BEWCurr']/@value")
                .evaluate(ch, XPathConstants.STRING));
        final int str = parse((String) xpath.compile("attribute[@name='STÄCurr']/@value")
                .evaluate(ch, XPathConstants.STRING));
        final String race = (String) xpath.compile("attribute[@name='Rasse']/@value")
                .evaluate(ch, XPathConstants.STRING);
        final int gk = parse((String) xpath.compile("//race[@name='" + race + "']/@GK")
                .evaluate(rules, XPathConstants.STRING));
        final int xp = parse((String) xpath.compile("attribute[@name='XPTotal']/@value")
                .evaluate(ch, XPathConstants.STRING));
        final int hg = (int) Math.floor(.5f + Math.sqrt((xp - 1) / 50f + .25));
        return Integer.toString(2 * 2 * 5 + bew + str - gk * 2 + hg * 2);
    }

    /**
     * Get total skill value.
     * @param ritem equipement item from rules
     * @param ch character element
     * @return total skill
     * @throws XPathExpressionException on error
     */
    private String getSkill(final Element ritem, final Element ch) throws XPathExpressionException {
        final String ranks = (String) xpath
                .compile(".//skill[@name='" + ritem.getAttribute("skill") + "']/@value")
                .evaluate(ch, XPathConstants.STRING);
        int sum = parse(ranks);
        final String[] attrs = ritem.getAttribute("attr").split("/");
        for (String attr : attrs) {
            final String val = (String) xpath
                    .compile(".//attribute[@name='" + attr + "Curr']/@value")
                    .evaluate(ch, XPathConstants.STRING);
            sum += parse(val);
        }
        return Integer.toString(sum);
    }
}
