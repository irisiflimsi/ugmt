package miju.rpg.ugmt;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML utilities for streaming.
 */
public interface XmlStreamsUtil {
    /**
     * Stream elements from a node list.
     * @param list list to extract from
     * @return element stream
     */
    static Stream<Element> elementStream(NodeList list) {
        return nodeStream(list).filter(n -> n instanceof Element).map(e -> (Element) e);
    }

    /**
     * Stream nodes from a node list.
     * @param list list to stream
     * @return node stream
     */
    static Stream<Node> nodeStream(NodeList list) {
        return IntStream.range(0, list.getLength()).mapToObj(list::item);
    }

    /**
     * Find the tags in the list which match condition and return what's mapped for it.
     * @param list list to analyze
     * @param tag tag to look for
     * @param map map after condition applied
     * @param condition condition to check before mapping
     * @param <T> type of map result
     * @return mapped elements found
     */
    static <T> List<T> getMappedTagsWithCondition(NodeList list, String tag, Function<Element, T> map, Predicate<Element> condition) {
        return getTagsFromList(list, tag).filter(e -> condition.test(e)).map(map)
                .collect(Collectors.toList());
    }

    /**
     * Find the tags in the list and return what's mapped for it.
     * @param list list to analyze
     * @param tag tag to look for
     * @param map map
     * @param <T> type of map result
     * @return mapped elements found
     */
    static <T> List<T> getMappedTags(NodeList list, String tag, Function<Element, T> map) {
        return getTagsFromList(list, tag).map(map).collect(Collectors.toList());
    }

    /**
     * Find the tags in the list.
     * @param list list to search
     * @param tag tag to look for
     * @return element (tag) stream
     */
    static Stream<Element> getTagsFromList(NodeList list, String tag) {
        return elementStream(list).filter(n -> n.getNodeName().equals(tag));
    }

    /**
     * Find the elements in the list which match condition and return it.
     * @param list list to analyze
     * @param map map after condition applied
     * @param condition condition to check before mapping
     * @return mapped elements found
     */
    static IntStream getMappedAllWithCondition(NodeList list, ToIntFunction<Element> map, Predicate<Element> condition) {
        return elementStream(list).filter(e -> condition.test(e)).mapToInt(map);
    }

    /**
     * Get first element which has attr = value.
     * @param node node to start search from
     * @param elemName tags to restrict to
     * @param attr attribute to match
     * @param val value to restrict to
     * @return first element found
     */
    static Element getElementByTagAndAttrEqVal(Node node, String elemName, String attr, String val) {
        return getElementFromListByAttrEqVal(getSelfOrDocElement(node).getElementsByTagName(elemName), attr, val);
    }

    /**
     * Get elements which have attr = value.
     * @param node node to start search from
     * @param elemName tags to restrict to
     * @param attr attribute to match
     * @param val value to restrict to
     * @return first element found
     */
    static List<Element> getElementsByTagAndAttrEqVal(Node node, String elemName, String attr, String val) {
        return getElementsFromListByAttrEqVal(getSelfOrDocElement(node).getElementsByTagName(elemName), attr, val);
    }

    /**
     * Get first element which has attr = value.
     * @param node node to start search from
     * @param attr attribute to match
     * @param val value to restrict to
     * @return first element found
     */
    static Element getElementByAttrEqVal(Node node, String attr, String val) {
        return getElementFromListByAttrEqVal(getSelfOrDocElement(node).getChildNodes(), attr, val);
    }

    /**
     * Get first element from node list, which has attr = value.
     * @param list node list to search
     * @param attr attr to check
     * @param val value to check
     * @return first element that matches.
     */
    static Element getElementFromListByAttrEqVal(NodeList list, String attr, String val) {
        return elementStream(list).filter(e -> e.getAttribute(attr).equals(val)).findFirst().orElse(null);
    }

    /**
     * Get elements from node list, which have attr = value.
     * @param list node list to search
     * @param attr attr to check
     * @param val value to check
     * @return first element that matches.
     */
    static List<Element> getElementsFromListByAttrEqVal(NodeList list, String attr, String val) {
        return elementStream(list).filter(e -> e.getAttribute(attr).equals(val)).collect(Collectors.toList());
    }

    /**
     * Get self or root element, if a document itself is passed.
     * @param node node to check
     * @return element
     */
    static Element getSelfOrDocElement(Node node) {
        if (node instanceof Document) {
            return ((Document) node).getDocumentElement();
        }
        return (Element) node;
    }
}
