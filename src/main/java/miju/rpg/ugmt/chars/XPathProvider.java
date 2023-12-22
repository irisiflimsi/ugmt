package miju.rpg.ugmt.chars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import static miju.rpg.ugmt.Utils.parse;
import static miju.rpg.ugmt.Utils.parseDbl;

import miju.rpg.ugmt.Utils;
import miju.rpg.ugmt.XmlStreamsUtil;

/**
 * Provides for XPATH in the rules and adds some utility function. (All
 * namespaces.) We provide a table-lookup, which actually should be a
 * sub-pattern for rules. rules allows to delegate to rules lookup. We need to
 * escape quotations here and use | for it.
 */
public class XPathProvider {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathProvider.class);

    /** XPath helper. */
    private XPathFactory xpathFactory;

    /** Rules. */
    private CharProxy charProxy;

    /** Context. */
    private String name;

    /** Functions. */
    private Map<String, XPathFunction> functions = new HashMap<>();

    /**
     * Constructor.
     * @param myCharProxy char proxy for lookup.
     */
    XPathProvider(final CharProxy myCharProxy) {
        charProxy = myCharProxy;
        System.setProperty("jdk.xml.xpathExprOpLimit", "0");
        xpathFactory = XPathFactory.newInstance();
        xpathFactory.setXPathFunctionResolver(new XPathFunctionResolver() {
            @Override
            public XPathFunction resolveFunction(final QName qname, final int arity) {
                final XPathFunction ret = functions.get(qname.getLocalPart());
                LOGGER.debug("Lookup function={}, ret={}", qname.getLocalPart(), ret);
                return ret;
            }
        });

        functions.put("year", args -> getYearFunction(getParams(args)));
        functions.put("mod", args -> getModFunction(getParams(args)));
        functions.put("raw", args -> getRawFunction(getParams(args)));
        functions.put("card", args -> getCardinalityFunction(getParams(args)));
        functions.put("table-lookup", args -> getTableLookupFunction(getParams(args)));
        functions.put("attribute", args -> getAttributeFunction(getParams(args)));
        functions.put("skill", args -> getSkillFunction(getParams(args)));
        functions.put("ifthenelse", args -> getIfThenElseFunction(getParams(args)));
        functions.put("parse", args -> getParseFunction(getParams(args)));
        functions.put("rules", args -> getRulesFunction(getParams(args)));
        functions.put("sign", args -> getSignFunction(getParams(args)));
        functions.put("max", args -> getMaxFunction(getParams(args)));
        functions.put("min", args -> getMinFunction(getParams(args)));
        functions.put("sum", args -> getSumFunction(getParams(args)));
        functions.put("rules-sum", args -> getRulesSumFunction(getParams(args)));
        functions.put("rules-max", args -> getRulesMaxFunction(getParams(args)));
        functions.put("rules-min", args -> getRulesMinFunction(getParams(args)));
        functions.put("eval", args -> getEvalFunction(getParams(args)));
    }

    /**
     * Context setter. Used to evaluate "$name" for skills and equipment.
     * @param aName string that classifies the context the xpaths shall be evaluated in.
     */
    public void setContextName(final String aName) {
        name = aName;
    }

    /**
     * Extract the year of a date in the form "day-month-year".
     * @param value one string
     * @return year of date
     */
    private Object getYearFunction(final List<String> value) {
        LOGGER.debug("year: value={}", value);
        if (value.size() > 0 && value.get(0).length() > 2) {
            return Double.parseDouble(value.get(0).substring(value.get(0).length() - 3));
        }
        else {
            return 0;
        }
    }

    /**
     * Extract the raw name of an equipment item.
     * @param value one string
     * @return raw name
     */
    private Object getRawFunction(final List<String> value) {
        if (value.size() > 0) {
            return value.get(0).replaceAll(Rules.REGEX_MOD, "").replaceAll(Rules.REGEX_NUM, "");
        }
        return "";
    }

    /**
     * Extract the cardinality of an equipment item.
     * @param value one string
     * @return cardinality
     */
    private Object getCardinalityFunction(final List<String> value) {
        LOGGER.debug("value={}", value);
        int ret = 1;
        try {
            ret = Integer.parseInt(value.get(0).replaceAll(Rules.REGEX_ANY + Rules.REGEX_NUM + Rules.REGEX_ANY, "$1"));
        }
        catch (Exception e) {
            // Ignore this
        }
        LOGGER.debug("ret={}", ret);
        return ret;
    }

    /**
     * Extract the modifier of an equipment item.
     * @param value one string
     * @return modifier
     */
    private Object getModFunction(final List<String> value) {
        LOGGER.debug("mod: value={}", value);
        if (value.size() > 0) {
            final Pattern pattern = Pattern.compile(Rules.REGEX_ANY + Rules.REGEX_MOD + Rules.REGEX_ANY);
            final Matcher m = pattern.matcher(value.get(0));
            if (m.find() && m.groupCount() > 0) {
                return m.group(1);
            }
        }
        return "";
    }

    /**
     * Lookup the table from first arg and numeric key from second. Tables must
     * provide keyL and keyH.
     * @param value table name and key
     * @return first entry that matches.
     */
    private Object getTableLookupFunction(final List<String> value) {
        LOGGER.debug("table-lookup: value={}", value);
        Object ret = "";
        switch (value.size()) {
        case 3:
            ret = charProxy.getRules().getRowValueInTableForKey(value.get(0),
                    parseDbl(value.get(1)), value.get(2));
            break;
        case 2:
            ret = charProxy.getRules().getRowValueInTableForKey(value.get(0),
                    parseDbl(value.get(1)), null);
            break;
        default:
        }
        LOGGER.debug("table-lookup: ret={}", ret);
        return ret;
    }

    /**
     * Evaluate a string for char proxy.
     * @param value evaluate this
     * @return evaluation
     */
    private Object getEvalFunction(final List<String> value) {
        LOGGER.debug("eval: value={}", value);
        Object ret = null;
        try {
            ret = compile(value.get(0)).evaluate(getRules().getRoot(), XPathConstants.STRING);
        }
        catch  (final XPathExpressionException e) {
            LOGGER.error("", e);
        }
        LOGGER.debug("eval: ret={}", ret);
        return ret;
    }

    /**
     * Lookup the rules with the specified xpath.
     * @param value rule to evaluate on "rules".
     * @return evaluated rules xpath.
     */
    private Object getRulesFunction(final List<String> value) {
        Object ret = "";
        LOGGER.debug("rules: value={}", value);
        final String rule = value.get(0);
        try {
            ret = compile(rule).evaluate(getRules().getRoot(), XPathConstants.STRING);
        }
        catch (final XPathExpressionException e) {
            LOGGER.error("", e);
        }
        LOGGER.debug("rules: ret={}", ret);
        return ret;
    }

    /**
     * Lookup a character attribute.
     * @param value attribute name
     * @return attribute value
     */
    private Object getAttributeFunction(final List<String> value) {
        LOGGER.debug("attribute: value[0]={}", value.get(0));
        final String ret = charProxy.getAttribute(value.get(0));
        LOGGER.debug("attribute: ret={}", ret);
        return ret;
    }

    /**
     * Lookup a character skill.
     * @param value skill name
     * @return skill value
     */
    private Object getSkillFunction(final List<String> value) {
        LOGGER.debug("skill: value={}", value);
        final String ret;
        if (value.get(1).equals(Rules.CLASS)) {
            ret = charProxy.getClassSkill(value.get(0));
        }
        else {
            ret = charProxy.getSkillAttribute(value.get(0), value.get(1));
        }
        LOGGER.debug("skill: ret={}", ret);
        return ret;
    }

    /**
     * Do if-then-else.
     * @param value clause, then-value and else-value
     * @return if first arg true then second arg else third
     */
    private Object getIfThenElseFunction(final List<String> value) {
        LOGGER.debug("ifthenelse: value={}", value);
        if (value.get(0).equals(Boolean.TRUE.toString())) {
            return value.get(1);
        }
        else {
            return value.get(2);
        }
    }

    /**
     * Parse an integer.
     * @param value one string
     * @return parsed value
     */
    private Object getParseFunction(final List<String> value) {
        LOGGER.debug("parse: value={}", value);
        Object ret = 0;
        if (value.size() > 0) {
            ret = parse(value.get(0));
        }
        LOGGER.debug("parse: ret={}", ret);
        return ret;
    }

    /**
     * Sign an integer.
     * @param value one string
     * @return signed value
     */
    private Object getSignFunction(final List<String> value) {
        LOGGER.debug("sign: value={}", value);
        int parsed = 0;
        if (value.size() > 0) {
            parsed = parse(value.get(0));
        }
        String ret = Integer.toString(parsed);
        if (parsed >= 0) {
            ret = "+" + parsed;
        }
        LOGGER.debug("sign: ret={}", ret);
        return ret;
    }

    /**
     * Max numbers.
     * @param value regex and list of strings to check
     * @return max value
     */
    private Object getMaxFunction(final List<String> value) {
        LOGGER.debug("max: value={}", value);
        final double ret = parseDblStream(value).max().orElse(Double.MIN_VALUE);
        LOGGER.debug("max: ret={}", ret);
        return typeCast(ret);
    }

    /**
     * Min numbers.
     * @param value regex and list of strings to check
     * @return min value
     */
    private Object getMinFunction(final List<String> value) {
        LOGGER.debug("min: value={}", value);
        final double ret = parseDblStream(value).min().orElse(Double.MAX_VALUE);
        LOGGER.debug("min: ret={}", ret);
        return typeCast(ret);
    }

    /**
     * Sum numbers.
     * @param value regex and list of strings to check
     * @return sum value
     */
    private Object getSumFunction(final List<String> value) {
        LOGGER.debug("sum: value={}", value);
        final double ret = parseDblStream(value).sum();
        LOGGER.debug("sum: ret={}", ret);
        return typeCast(ret);
    }

    /**
     * Lookup the rules with the specified xpath and sum.
     * @param value regex and list of strings to check
     * @return sum of evaluation on rules.
     */
    private Object getRulesSumFunction(final List<String> value) {
        LOGGER.debug("rulessum: value={}", value);
        final double ret = evaluateRulesStream(value).sum();
        LOGGER.debug("rulessum: ret={}", ret);
        return ret;
    }

    /**
     * Lookup the rules with the specified xpath and max.
     * @param value regex and list of strings to check
     * @return max of evaluation on rules.
     */
    private Object getRulesMaxFunction(final List<String> value) {
        LOGGER.debug("rulesmax: value={}", value);
        final double ret = evaluateRulesStream(value).max().orElse(Double.MIN_VALUE);
        LOGGER.debug("rulesmax: ret={}", ret);
        return ret;
    }

    /**
     * Lookup the rules with the specified xpath and min.
     * @param value regex and list of strings to check
     * @return min of evaluation on rules.
     */
    private Object getRulesMinFunction(final List<String> value) {
        LOGGER.debug("rulesmin: value={}", value);
        final double ret = evaluateRulesStream(value).min().orElse(Double.MAX_VALUE);
        LOGGER.debug("rulesmin: ret={}", ret);
        return ret;
    }

    /**
     * Utility.
     * @param rule rule to compile
     * @return new xpath
     * @throws XPathExpressionException on error
     */
    public XPathExpression compile(final String rule) throws XPathExpressionException {
        String compile = rule.replaceAll(Matcher.quoteReplacement("$name"), name);
        compile = unquote(compile);
        LOGGER.debug("compile={}", compile);
        return xpathFactory.newXPath().compile(compile);
    }

    /**
     * Unquote "'" on demand.
     * @param str string to unquote.
     * @return unquoted string
     */
    private String unquote(final Object str) {
        if (!str.toString().contains("'")) {
            return str.toString().replace('|', '\'');
        }
        return str.toString();
    }

    /**
     * Helper to extract arguments from the untyped list from
     * javax.xml.xpath.XPathFunction's evaluate.
     * @param args argument list from xpath function
     * @return stringified arguments
     */
    private List<String> getParams(final List<?> args) {
        LOGGER.debug("getParams args={}", args);
        final List<String> ret = new ArrayList<>();
        if (args instanceof NodeList) {
            LOGGER.debug("getParams args.length={}", ((NodeList) args).getLength());
            return XmlStreamsUtil.nodeStream((NodeList) args)
                    .map(n -> n.getNodeValue()).collect(Collectors.toList());
        }
        else {
            for (Object o : args) {
                if (o instanceof NodeList) {
                    final NodeList nodelist = (NodeList) o;
                    LOGGER.debug("getParams nodelist.length={}", nodelist.getLength());
                    final List<String> list = XmlStreamsUtil.nodeStream(nodelist)
                            .map(n -> n.getNodeValue()).map(n -> unquote(n))
                            .collect(Collectors.toList());
                    LOGGER.debug("getParams list={}", list);
                    ret.addAll(list);
                }
                else {
                    final String s = unquote(o);
                    LOGGER.debug("getParams s={}", s);
                    ret.add(s);
                }
            }
        }
        return ret;
    }

    /**
     * Get char proxy's rules.
     * @return char proxy's rules
     */
    private Rules getRules() {
        return charProxy.getRules();
    }

    /**
     * Evaluates a stream of rules, unquoting "pipes" in the process.
     * @param value list to stream
     * @return stream of doubles evaluated from rules
     */
    private DoubleStream evaluateRulesStream(final List<String> value) {
        final String rule = value.remove(0).replace('|', '\'');
        return value.stream()
                .map(s -> rule.replaceAll(Matcher.quoteReplacement("$1"), s))
                .map(s -> evaluateRules(s)).mapToDouble(Double::doubleValue);
    }

    /**
     * Evaluate a rules string and parse the result as double. Exceptions are
     * ignored and return 0.
     * @param s string to evaluate in rules
     * @return number result of evaluation
     */
    private double evaluateRules(final String s) {
        try {
            return parseDbl((String) compile(s).evaluate(getRules().getRoot(), XPathConstants.STRING));
        }
        catch (final XPathExpressionException e) {
            LOGGER.error("", e);
            return 0;
        }
    }

    /**
     * Return a stream of doubles for a list (the elements of which that can be evaluated).
     * @param value list to stream
     * @return stream of doubles parsed from list
     */
    private DoubleStream parseDblStream(final List<String> value) {
        return value.stream().map(Utils::parseDbl).mapToDouble(Double::doubleValue);
    }

    /**
     * Cast a double to int, if it is sufficiently close to one.
     * @param arg double to check/cast
     * @return (boxed) integer or double
     */
    public Object typeCast(final double arg) {
        if (Math.abs(arg % 1) < Utils.PRECISION) {
            return (int) arg;
        }
        return arg;
    }
}
