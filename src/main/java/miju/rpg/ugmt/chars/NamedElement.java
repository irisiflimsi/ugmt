package miju.rpg.ugmt.chars;

import static miju.rpg.ugmt.XmlNames.Attributes.NAME;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class allows to sort elements according to <b>name</b>.
 */
public final class NamedElement implements Comparable<NamedElement> {
    /** Main element. */
    private Element proxied;

    /**
     * Constructor.
     * @param elem proxied element
     */
    public NamedElement(final Element elem) {
        this.proxied = elem;
    }

    /**
     * Proxy method.
     * @return proxied.getParentNode();
     */
    public Node getParentNode() {
        return proxied.getParentNode();
    }

    /**
     * Proxy method.
     * @param name name
     * @return proxied.getAttribute(name);
     */
    public String getAttribute(final String name) {
        return proxied.getAttribute(name);
    }

    @Override
    public int compareTo(final NamedElement arg) {
        final int v = proxied.getAttribute(NAME).compareTo(arg.proxied.getAttribute(NAME));
        if (v != 0) {
            return v;
        }
        return proxied.compareDocumentPosition(arg.proxied);
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg instanceof NamedElement) {
            final NamedElement other = (NamedElement) arg;
            return proxied.getAttribute(NAME).equals(other.proxied.getAttribute(NAME))
                    && proxied.compareDocumentPosition(other.proxied) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return proxied.getAttribute(NAME).hashCode();
    }
}
