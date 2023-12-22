package miju.rpg.ugmt.chars;

import static miju.rpg.ugmt.XmlNames.Attributes.TYPE;

import java.text.Collator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class parses a text files (usually XML) and replaces any occurrence of
 * "$Var", where "Var" may be any string starting with a letter - a typical
 * variable indication in scripts. The rules for replacements are explained in
 * the transform method as they occur.
 */
class SimpleSheetTransformer {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSheetTransformer.class);

    /** Helper string constant. */
    private static final String REGEX_NAME = "[a-zäüößA-ZÄÖÜ -/]*";

    /** Helper string constant. */
    private static final String REGEX_LITERAL_DOLLAR = "\\$";

    /** Helper string constant. */
    private static final String EQ = "Eq";

    /** Helper string constant. */
    private static final String EQ_PREFIX = REGEX_LITERAL_DOLLAR + EQ;

    /** Helper string constant. */
    private static final String SK = "Sk";

    /** Helper string constant. */
    private static final String SK_PREFIX = REGEX_LITERAL_DOLLAR + SK;

    /** Helper string constant. */
    private static final String SPELL_PREFIX = REGEX_LITERAL_DOLLAR + "Spell";

    /**
     * Transformation process.
     * @param c character proxy
     * @param template template to transform through
     * @return transformed character
     */
    String transform(final CharProxy c, final String template) {
        String out = template;

        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Name", c.getName());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Id", c.getId());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Portrait", c.getPortrait());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Marker", c.getMarker());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Credits", c.getCredits());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Psyche", c.getPsyche());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Medical", c.getMedical());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "Background", c.getBackground());
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "ShortNote-1", c.getShortNote(1));
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "ShortNote-2", c.getShortNote(2));
        out = out.replaceAll(REGEX_LITERAL_DOLLAR + "ShortNote-3", c.getShortNote(3));

        // Replace attributes. Name of attributes.
        for (String attr : c.getAttributes()) {
            out = out.replaceAll(REGEX_LITERAL_DOLLAR + attr, c.getAttribute(attr));
        }

        // Replace Spells. Name of spell with prefix "Spell-" and
        // added "N" for name and "V" for skill level
        out = transformSpells(c, out);

        final Map<String, Integer> idx = new HashMap<String, Integer>();

        // Replace skills.
        out = transformSkills(c, template, out, idx);

        // Replace equipment
        out = transformEquipment(c, template, out, idx);

        // Clean out. Clean out access commas too.
        return out.replaceAll(",?\\s*\\$[a-zäöüßA-ZÄÖÜ0-9/,. -]*", "");
    }

    /**
     * Transform skills in character. Skills in the rules are organized as
     * "Section/Subsection/.../SkillName#Specialization", where subsections may
     * be quite long but also missing. Specialization is optional too. To make
     * replacement adequate (unfortunately not simple) the following is used:
     * Replace the detailed variable names and then the general one. Names in
     * the template look like "Sk" then the section name and then "N" and "V" as
     * before. They are numbered and access skills must go to the general list.
     * Alphabetical ordering is used within each section. To make skill names
     * short on the display, context knowledge is assumed. Section names are
     * dropped, specialization is shown in parenthesis, if present. Sections can
     * be grouped by concatenation in the templates.
     * @param c character proxy
     * @param template template that contains the original text
     * @param preOut pre-transformed text
     * @param idx index map (maps each sub-type to the currently last integer)
     * @return transformed text
     */
    private String transformSkills(final CharProxy c, final String template, final String preOut, final Map<String, Integer> idx) {
        String out = preOut;
        Matcher m;
        final SortedSet<String> keys = new TreeSet<String>(Collator.getInstance(Locale.FRANCE));
        keys.addAll(c.getSkills());
        for (String key : keys) {
            // Group will contain the top level hierarchy name. This will
            // be used, in the general skill sections to allow separation
            String group = null;

            if (key.indexOf("/") > -1) {
                final int fi = key.indexOf("/");
                group = key.substring(0, fi);
            }

            // Section will contain the infix with which to look for in the
            // template
            String section = c.getRules().getSection(key);

            // Sections can be either the top hierarchy of the skill
            // (e.g. "Weapon") or the section itself (e.g. "Combat Skills")
            if (group != null) {
                m = Pattern.compile(SK_PREFIX + REGEX_NAME + group + REGEX_NAME + "-").matcher(template);
                if (m.find()) {
                    section = group;
                }
            }

            // Find the group to count internally
            m = Pattern.compile(SK_PREFIX + REGEX_NAME + section + REGEX_NAME + "-").matcher(template);
            if (m.find()) {
                section = m.group().substring(3, m.group().length() - 1);
                // Get internal numbering
                Integer ii = (Integer) idx.get(section);
                if (ii == null) {
                    ii = Integer.valueOf(1);
                }

                // Replace
                out = c.replaceAllSkills(SK_PREFIX + section + "-" + ii.intValue(), out, key, section);

                idx.put(section, Integer.valueOf(ii + 1));
            }
            else {
                // Find general skills
                m = Pattern.compile(SK_PREFIX + "-").matcher(template);

                if (m.find()) {
                    // Get internal numbering
                    Integer ii = (Integer) idx.get(SK);
                    if (ii == null) {
                        ii = Integer.valueOf(1);
                    }

                    // Replace
                    out = c.replaceAllSkills(SK_PREFIX + "-" + ii.intValue(), out, key, section);

                    idx.put(SK, Integer.valueOf(ii + 1));
                }
            }
        }
        return out;
    }

    /**
     * Transform equipment in character. Equipment has the form
     * <em>Name (Mod-a) (Mod-b)</em>, where <em>(Mod-b)</em> is optional. If the
     * last - either <em>Mod-a</em> or <em>Mod-b</em> - is a number, this will
     * be used to multiply the weight of the item for calculation of total
     * weight.
     * Equipment is also searched according to type.
     * @param c character proxy
     * @param template template that contains the original text
     * @param preOut pre-transformed text
     * @param idx index map (maps each sub-type to the currently last integer)
     * @return transformed text
     */
    private String transformEquipment(final CharProxy c, final String template, final String preOut, final Map<String, Integer> idx) {
        String out = preOut;
        Matcher m;
        final SortedSet<NamedElement> equipment = c.getEquipment();
        for (NamedElement item : equipment) {
            final String section = item.getAttribute(TYPE);
            LOGGER.debug("pre={}", EQ_PREFIX + section);
            m = Pattern.compile(EQ_PREFIX + section).matcher(out);
            if (m.find()) {
                // Get internal numbering
                Integer ii = (Integer) idx.get(EQ + section);
                if (ii == null) {
                    ii = Integer.valueOf(1);
                }

                // Replace
                out = c.replaceAllEquipment(EQ_PREFIX + section + "-" + ii.intValue(), out, item);

                idx.put(EQ + section, Integer.valueOf(ii + 1));
            }
            else {
                m = Pattern.compile(EQ_PREFIX + "-").matcher(template);
                if (m.find()) {
                    // Get internal numbering
                    Integer ii = (Integer) idx.get(EQ);
                    if (ii == null) {
                        ii = Integer.valueOf(1);
                    }

                    // Replace
                    out = c.replaceAllEquipment(EQ_PREFIX + "-" + ii.intValue(), out, item);

                    idx.put(EQ, Integer.valueOf(ii + 1));
                }
            }
        }
        return out;
    }

    /**
     * Transform spells in character.
     * @param c character proxy
     * @param preOut pre-transformed text
     * @return transformed text
     */
    private String transformSpells(final CharProxy c, final String preOut) {
        String out = preOut;
        int i = 1;
        for (String spell : c.getSpells()) {
            LOGGER.info("spell={}, i={}", spell, i);
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "CT", c.getSpellAttr("time", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "CN", c.getSpellAttr("cast", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "LV", c.getSpellAttr("level", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "A", c.getSpellAttr("attr", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "D", c.getSpellAttr("description", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "F", c.getSpellAttr("focus", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "I", c.getSpellAttr("ingredients", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "L", c.getSpellAttr("learn", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "N", spell);
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "R", c.getSpellAttr("range", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "S", c.getSpellAttr("school", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "V", c.getSpellAttr("value", spell));
            out = out.replaceAll(SPELL_PREFIX + "-" + i + "d", c.getSpellAttr("duration", spell));
            i++;
        }
        return out;
    }
}
