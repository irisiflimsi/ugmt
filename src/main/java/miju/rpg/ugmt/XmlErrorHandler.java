package miju.rpg.ugmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * XML Error handler.
 */
public class XmlErrorHandler implements ErrorHandler {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXmlProvider.class);

    /**
     * Error method.
     * @param e exception to handle
     */
    public void error(final SAXParseException e) {
        LOGGER.error("", e);
    }

    /**
     * Fatal error method.
     * @param e exception to handle
     */
    public void fatalError(final SAXParseException e) {
        LOGGER.error("", e);
    }

    /**
     * Warning method.
     * @param e exception to handle
     */
    public void warning(final SAXParseException e) {
        LOGGER.error("", e);
    }
}
