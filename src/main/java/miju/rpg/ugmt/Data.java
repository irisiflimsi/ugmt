package miju.rpg.ugmt;

import static miju.rpg.ugmt.XmlNames.Attributes.ID;
import static miju.rpg.ugmt.XmlNames.Elements.DATA;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The data information from XML.
 */
public class Data extends AbstractXmlProvider {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Data.class);

    /** Helper constant string. */
    private static final Path DATA_ABS_PATH = ROOT_ABS_PATH.resolve("data");

    /** File to save state in. */
    private static final Path STATE_ABS_PATH = DATA_ABS_PATH.resolve("state.xml");

    /**
     * Constructor.
     * @throws ParserConfigurationException on error
     * @throws IOException on error
     * @throws SAXException on error
     */
    public Data() throws ParserConfigurationException, IOException, SAXException {
        super();
        load(getDataFiles(DATA_ABS_PATH));
        LOGGER.warn("Ready...");
        Monitor.monitor(this, DATA_ABS_PATH);
    }

    /**
     * Return all data files.
     * @param absPath directory in which to search
     * @return all data files
     * @throws IOException on error
     */
    private DirectoryStream<Path> getDataFiles(final Path absPath) throws IOException {
        return Files.newDirectoryStream(absPath, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(final Path entry) throws IOException {
                return (entry != null && entry.toString().endsWith(".xml"));
            }
        });
    }

    /**
     * This general purpose routine could be extracted if needed elsewhere.
     * @param list to join to a string
     * @return concatenated set
     */
    public static String join(final Collection<String> list) {
        final StringBuilder sb = new StringBuilder();
        for (String item : list) {
            sb.append(item).append(",");
        }
        return sb.substring(0, Math.max(0, sb.length() - 1)); // Sheaves of last comma
    }

    /**
     * Put the element to state file.
     * @param elem element to save
     * @throws TransformerException on error
     */
    void save(final Element elem) throws TransformerException {
        final Document newDoc = newDocument();
        final Element newRoot = newDoc.createElement(DATA);
        newRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        newRoot.setAttribute("xsi:noNamespaceSchemaLocation", "./ugmt.xsd");
        newDoc.appendChild(newRoot);
        if (elem != null) {
            final Node newElem = pathToRoot(newDoc, elem);
            newRoot.appendChild(newElem.getFirstChild());
        }
        transform(newDoc, STATE_ABS_PATH);
    }

    @Override
    public void load(final Path absPath) throws SAXException, IOException {
        load(Collections.singleton(absPath));
    }

    /**
     * Load files.
     * @param list file list
     * @throws IOException on error
     * @throws SAXException on error
     */
    private void load(final DirectoryStream<Path> list) throws SAXException, IOException {
        load((Iterable<Path>) list);
    }

    /**
     * Load files.
     * @param iterable file list
     * @throws IOException on error
     * @throws SAXException on error
     */
    private synchronized void load(final Iterable<Path> iterable) throws SAXException, IOException {
        final Document tmpData = newDocument();
        tmpData.appendChild(tmpData.createElement(DATA));

        for (Path absPath : iterable) {
            LOGGER.info("Loading... absPath={}", absPath);
            parse(tmpData, parse(absPath));
        }

        setRoot(tmpData);
    }

    /**
     * Parse a document and merge it. Observe uniqueness.
     * @param toDoc document into which to add doc
     * @param fromDoc document to add into sum
     * @return newly merged nodes
     * @throws Exception on error
     */
    private List<Element> parse(final Document toDoc, final Document fromDoc) {
        final NodeList fromList = fromDoc.getDocumentElement().getChildNodes();

        final List<Element> ret = new ArrayList<Element>();
        int i = -1;
        while (i < fromList.getLength()) {
            final Node fromNode = fromList.item(i);
            if (!(fromNode instanceof Element)) {
                i = i + 1;
                continue;
            }
            Element fromElement = (Element) fromNode;
            final Element toElement = (Element) toDoc.getElementById(fromElement.getAttribute(ID));
            // This modifies the fromList, but preserves the xml:id
            fromElement = (Element) toDoc.adoptNode(fromElement);
            fromElement.setIdAttribute(ID, true);
            if (toElement == null) {
                toDoc.getDocumentElement().appendChild(fromElement);
            }
            else {
                toDoc.getDocumentElement().replaceChild(fromElement, toElement);
            }
            ret.add(fromElement);
        }
        return ret;
    }

    /**
     * Create a path to the root of an element.
     * @param newDoc document to create path in
     * @param oldElem element to create path for
     * @return root node of the new path
     */
    private synchronized Node pathToRoot(final Document newDoc, final Element oldElem) {
        Node newElem = newDoc.importNode(oldElem, true);
        Node oldParent = oldElem.getParentNode();
        while (!oldParent.equals(getRoot())) {
            // Create new parent clone
            final Node newParent = newDoc.importNode(oldParent, false);
            newParent.appendChild(newElem);
            // Recurse
            newElem = newParent;
            oldParent = oldParent.getParentNode();
        }
        return newElem;
    }
}
