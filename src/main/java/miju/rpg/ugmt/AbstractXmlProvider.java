package miju.rpg.ugmt;

import static miju.rpg.ugmt.XmlNames.Attributes.NAME;
import static miju.rpg.ugmt.XmlNames.Elements.TAG;
import static miju.rpg.ugmt.XmlNames.Elements.TAGGED;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML method container and reader of XML files. Used by rules readers and data
 * file readers.
 */
public abstract class AbstractXmlProvider implements XmlStreamsUtil {
    /** root dir. */
    public static final Path ROOT_ABS_PATH;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXmlProvider.class);

    /** Document builder. */
    private static DocumentBuilder docBuilder;

    /** XML transforming. */
    private static final TransformerFactory TRANS_FACTORY = TransformerFactory.newInstance();

    /** Data Document. */
    private Document root;

    static {
        try {
            ROOT_ABS_PATH = Paths.get(new URL(System.getProperty("ROOT")).toURI());
        }
        catch (final MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor.
     * @throws ParserConfigurationException on error
     */
    public AbstractXmlProvider() throws ParserConfigurationException {
        synchronized (AbstractXmlProvider.class) {
            if (docBuilder == null) {
                final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(true);
                dbf.setIgnoringElementContentWhitespace(true);
                dbf.setAttribute(
                        "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                        "http://www.w3.org/2001/XMLSchema");
                docBuilder = dbf.newDocumentBuilder();
                docBuilder.setErrorHandler(new XmlErrorHandler());
            }
        }
    }

    /**
     * Resolve a query/URL according to local files.
     * @param query query to localize
     * @return absolute file system path representing the query
     */
    public static Path string2file(final String query) {
        return Data.ROOT_ABS_PATH.resolve(Paths.get(query.replaceAll("/", File.separator)));
    }

    /**
     * Utility.
     * @param absPath XML file
     * @throws IOException I/O problem
     * @throws SAXException XML problem
     * @return XML document
     */
    public static Document parse(final Path absPath) throws SAXException, IOException {
        return docBuilder.parse(absPath.toFile());
    }

    /**
     * Utility.
     * @return new XML document
     */
    protected static Document newDocument() {
        return docBuilder.newDocument();
    }

    /**
     * Transform node according to XSL file referenced by fileName.
     * @param node node to transform
     * @param relPath XSL file
     * @return transformed string
     * @throws TransformerException on error
     */
    protected static String transform(final Node node, final Path relPath) throws TransformerException {
        final Path absPath = ROOT_ABS_PATH.resolve(relPath);
        final StringWriter result = new StringWriter();
        final Transformer transformer = TRANS_FACTORY.newTemplates(new StreamSource(absPath.toFile())).newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, XmlNames.YES);
        transformer.transform(new DOMSource(node), new StreamResult(result));
        // The last is a workaround
        return result.toString().replaceAll("xmlns=\"\"", "");
    }

    /**
     * Load a file. Shall be synchronized.
     * @param absPath file to load
     * @throws IOException on error
     * @throws SAXException on error
     */
    public abstract void load(Path absPath) throws SAXException, IOException;

    /**
     * Utility.
     * @param name id too look for
     * @return element with that id
     */
    public synchronized Element getElementById(final String name) {
        return getRoot().getElementById(name);
    }

    /**
     * Utility.
     * @return data root
     */
    public synchronized Document getRoot() {
        return root;
    }

    /**
     * Utility.
     * @param doc root
     */
    public synchronized void setRoot(final Document doc) {
        root = doc;
    }

    /**
     * Temporarily add all nodes to root level. Return the transformed result.
     * @param relPath transforming XSL file
     * @param nodes nodes to clone and append to root
     * @return return augmented, transformed, and stringified document
     * @throws Exception on error
     */
    synchronized String transformAllDataWithForeignNodes(final Path relPath, final Node... nodes) throws Exception {
        final List<Node> newNodeList = new ArrayList<Node>();
        try {
            for (Node node : nodes) {
                final Node newNode = root.importNode(node, true);
                root.getDocumentElement().appendChild(newNode);
                newNodeList.add(newNode);
            }
            return transform(root, relPath);
        }
        finally {
            for (Node newNode : newNodeList) {
                root.getDocumentElement().removeChild(newNode);
            }
        }
    }

    /**
     * Generates a new node tree for all <b>tag</b>-elements that are tagged
     * with <em>keys</em>. Generates a new node tree for all tags that are tagged with key
     * @param tag XML tag to look for
     * @param keys tag-chain (key is first)
     * @param gm all or just permit
     * @return new tree
     * @throws XPathExpressionException on error
     */
    Node copyNodesByTagsAndKey(final String tag, final String keys, final boolean gm) throws XPathExpressionException {
        return copyNodesByPredicate(tag, keys, gm);
    }

    /**
     * Generates a new node tree for all tags that are somewhere in the
     * <b>list</b>.
     * @param keys tag-chain
     * @param gm all or just permit
     * @param list nodes to copy from
     * @return new tree
     * @throws XPathExpressionException on error
     */
    Node createTagsByKey(final String keys, final boolean gm, final NodeList list) throws XPathExpressionException {
        final Set<String> tagSet = XmlStreamsUtil.elementStream(list)
                .flatMap(e -> XmlStreamsUtil.elementStream(e.getElementsByTagName(TAGGED)))
                .map(e -> e.getTextContent()).collect(Collectors.toSet());
        return createTags(tagSet);
    }

    /**
     * Generates a new node tree for all <b>elemName</b>-elements that are
     * tagged with the key-chain and satisfy predicate. The result elements all
     * carry the key-chain in the <em>chain</em> attribute, with their own name
     * prepended. If we are looking for "tag" itself, the <em>chain</em> is
     * reduced from all prepended attributes instead.
     * TODO what is the intention? Isn't clear anymore.
     * @param elemName XML element name to look for
     * @param keys key-chain
     * @param predicate predicate to check for inclusion in return list
     * @param gm all or just permit
     * @return new tree
     * @throws XPathExpressionException on error
     */
    private synchronized Node copyNodesByPredicate(final String elemName, final String keys, final boolean gm) throws XPathExpressionException {
        final Document newDoc = newDocument();
        final Element newRoot = newDoc.createElement("doc_" + elemName);

        // create xpath for tag & keys
        String xpath = "//" + elemName + "[";

        if (gm) {
            xpath += "true() and ";
        }
        else {
            xpath += "@permit='true' and ";
        }

        if (keys != null) {
            xpath += Stream.of(keys.split(",")).map(s -> "tagged/text()='" + s + "'")
                    .collect(Collectors.joining(" and ")) + " and ";
        }
        xpath += "true()]";

        LOGGER.debug("xpath={}", xpath);

        // Evaluate xpath
        final XPathExpression xpathExpression = XPathFactory.newInstance().newXPath().compile(xpath);
        final NodeList list = (NodeList) xpathExpression.evaluate(getRoot(), XPathConstants.NODESET);
        LOGGER.debug("list.length={}", list.getLength());

        // Check predicate
        for (int i = 0; i < list.getLength(); i++) {
            final Element newElem = (Element) newDoc.importNode(list.item(i), true);
            newRoot.appendChild(newElem);
        }

        return newRoot;
    }

    /**
     * Create XML tags from a list.
     */
    private synchronized Node createTags(Set<String> tagSet) throws XPathExpressionException {
        final Document newDoc = newDocument();
        final Element newRoot = newDoc.createElement("doc_tag");
        LOGGER.debug("tagSet.size={}", tagSet.size());

        for (String tag : tagSet) {
            final Element newElem = newDoc.createElement(TAG);
            newElem.setAttribute(NAME, tag);
            newRoot.appendChild(newElem);
        }
        return newRoot;
    }
}
