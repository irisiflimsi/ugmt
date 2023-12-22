package miju.rpg.ugmt.chars.handlers;

import static miju.rpg.ugmt.XmlNames.Attributes.VALUE;
import static miju.rpg.ugmt.XmlNames.Elements.SKILL;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.XmlStreamsUtil;
import miju.rpg.ugmt.chars.CharProxy;

/**
 * Delegate for certain character tags/entities.
 */
public class SkillHandler {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillHandler.class);

    /** Controller. */
    private CharProxy charProxy;

    /** Cache. */
    private Map<String, String> cache = Collections.synchronizedMap(new HashMap<>());

    /**
     * Contructor.
     * @param aCharProxy controller
     */
    public SkillHandler(final CharProxy aCharProxy) {
        charProxy = aCharProxy;
    }

    /**
     * Get all skills.
     * @return all skills
     */
    public List<String> getSkills() {
        return charProxy.getTags(SKILL);
    }

    /**
     * Get all skills.
     * @return all skills
     */
    public List<Element> getSkillElements() {
        return XmlStreamsUtil.getMappedTags(charProxy.getCharNodes(), SKILL, e -> e);
    }

    /**
     * Get the attribute "attr" from the "name" skill.
     * @param name skill to look in
     * @param attr attribute to look for
     * @return value of attr
     */
    public String getSkillAttribute(final String name, final String attr) {
        return getSkillAttribute(name, attr, false);
    }

    /**
     * Get the attribute "attr" from the "skillName" skill.
     * @param skillName skill to look in
     * @param attrName attribute to look for
     * @param force force rule evaluation
     * @return value of attr
     */
    String getSkillAttribute(final String skillName, final String attrName, final boolean force) {
        LOGGER.debug("name={}, attr={} force={}", skillName, attrName, force);
        String ret = cache.get(skillName + "@" + attrName + "@" + force);
        if (ret == null) {
            ret = charProxy.getAttrForNamedElem(SKILL, skillName, attrName);
            LOGGER.debug("interim ret={}", ret);
            try {
                final List<String> subnames = Arrays.asList(skillName.split("/"));
                Collections.reverse(subnames);
                for (String subname : subnames) {
                    if (ret == null || ret.length() == 0 || force) {
                        ret = charProxy.getRulesSkillAttribute(skillName, attrName, subname);
                    }
                }
            }
            catch (final Exception e) {
                LOGGER.error("", e);
            }
            LOGGER.debug("ret={}", ret);
            cache.put(skillName + "@" + attrName + "@" + force, ret);
        }
        return ret;
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
    public String replaceAllSkills(final String pre, final String preOut, final String skill, final String section) {
        String out = preOut;
        final String[] path = skill.split("/");
        final String ranks = getSkillAttribute(skill, "ranks");
        out = out.replaceAll(pre + "CL", getSkillAttribute(skill, "class"));
        out = out.replaceAll(pre + "TA", getSkillTalents(path[path.length - 1]));
        out = out.replaceAll(pre + "VV", getSkillAttribute(skill, VALUE, true));
        out = out.replaceAll(pre + "X1", getSkillAttribute(skill, "learn1"));
        out = out.replaceAll(pre + "X2", getSkillAttribute(skill, "learn2"));
        out = out.replaceAll(pre + "X3", getSkillAttribute(skill, "learn3"));
        out = out.replaceAll(pre + "X4", getSkillAttribute(skill, "learn4"));
        out = out.replaceAll(pre + "XP", prefix(ranks));
        out = out.replaceAll(pre + "XS", suffix(ranks));
        out = out.replaceAll(pre + "A", getSkillAttribute(skill, "attr"));
        out = out.replaceAll(pre + "C", charProxy.getClassSkill(path[path.length - 1]));
        out = out.replaceAll(pre + "L", getSkillAttribute(skill, "level"));
        out = out.replaceAll(pre + "M", getSkillAttribute(skill, "misc"));
        String name = getSkillAttribute(skill, "display").replace("&", "&amp;");
        out = out.replaceAll(pre + "N", name.substring(name.lastIndexOf("/") + 1));
        out = out.replaceAll(pre + "P", ranks);
        out = out.replaceAll(pre + "R", getSkillAttribute(skill, VALUE));
        out = out.replaceAll(pre + "S", getSkillAttribute(skill, "skill"));
        out = out.replaceAll(pre + "T", getSkillAttribute(skill, "total"));
        out = out.replaceAll(pre + "V", getSkillAttribute(skill, "value"));
        return out;
    }

    /**
     * String before '/'.
     * @param str string to extract prefix from
     * @return prefix
     */
    private static String suffix(final String str) {
        if (str.indexOf('/') > 0) {
            return str.substring(str.indexOf('/') + 1);
        }
        else {
            return str;
        }
    }

    /**
     * String after '/'.
     * @param str string to extract suffix from
     * @return suffix
     */
    private static String prefix(final String str) {
        if (str.indexOf('/') > 0) {
            return str.substring(0, str.indexOf('/'));
        }
        else {
            return str;
        }
    }

    /**
     * Get talents relevant for skills.
     * @param skillName skill name
     * @return list of talents the character has
     */
    private String getSkillTalents(final String skillName) {
        final SortedSet<String> charSkills = new TreeSet<String>(getSkills());
        final SortedSet<String> talentSkills = new TreeSet<String>(charProxy.getRules().getSkillTalents(skillName));
        charSkills.retainAll(talentSkills);
        return Data.join(charSkills);
    }
}
