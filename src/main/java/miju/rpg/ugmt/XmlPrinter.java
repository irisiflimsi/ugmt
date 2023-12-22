package miju.rpg.ugmt;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Debug helper to "pretty print" XML/DOM documents.
 */
public final class XmlPrinter {
    /** Logger. */
    static final Logger LOGGER = LoggerFactory.getLogger(XmlPrinter.class);

    /** XML transforming. */
    private static final TransformerFactory TRANS_FACTORY = TransformerFactory.newInstance();

    /** Hide constructor. */
    private XmlPrinter() {
    }

    /**
     * Helper for debugging.
     * @param node node to print
     * @return stringified node with children
     */
    public static String printNodeChildren(final Node node) {
        final StringBuilder ret = new StringBuilder();
        final NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            ret.append(printNode(nl.item(i)));
        }
        return ret.toString();
    }

    /**
     * Helper for debugging.
     * @param node node to print
     * @return stringified node
     */
    public static String printNode(final Node node) {
        final Transformer transformer;
        try {
            transformer = TRANS_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, XmlNames.YES);
            transformer.setOutputProperty(OutputKeys.INDENT, XmlNames.YES);

            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final DOMSource source = new DOMSource(node);
            transformer.transform(source, result);
            return writer.toString();
        }
        catch (final TransformerException e) {
            LOGGER.error("", e);
        }
        return null;
    }


}
