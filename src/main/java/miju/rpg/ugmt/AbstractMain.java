package miju.rpg.ugmt;

import static miju.rpg.ugmt.XmlNames.Attributes.ID;
import static miju.rpg.ugmt.XmlNames.Attributes.PERMIT;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Base class for programmatic extensions of various plugins.
 */
public abstract class AbstractMain {
    /** common data access. */
    private Data data;

    /**
     * Setter.
     * @param theData data to set
     */
    public void setData(final Data theData) {
        this.data = theData;
    }

    /**
     * Getter.
     * @return all data
     */
    public Data getData() {
        return data;
    }

    /**
     * Easy distinction for content-type.
     * @param args parameter to look for
     * @return content-type
     * @throws UnsupportedEncodingException parameter can't be decoded
     */
    public String getContentType(final HttpQueryParams args) throws UnsupportedEncodingException {
        return "application/xml; charset=utf-8";
    }

    /**
     * Get the <b>plugin-id</b>, which is the last part before Main.
     * @return plugin id
     */
    public String getPluginId() {
        final String[] parts = getClass().getPackage().getName().split(Pattern.quote("."));
        return parts[parts.length - 1];
    }

    /**
     * Main content method.
     * @param args parameter to parse
     * @param gm GM query?
     * @return content to push on stream
     * @throws Exception on error
     */
    public Object getContent(final HttpQueryParams args, final boolean gm) throws Exception {
        // Get all the parameters we know.
        final String edit = args.getEdit();
        final String value = args.getVal();
        final String key = args.getKey();
        final String save = args.getSave();
        final String view = args.getView();
        final String tag = args.getTag();
        final String tmpl = args.getTmpl();

        // 1. Change permissions
        if (edit != null && gm) {
            final Element elem = XmlStreamsUtil.getElementByAttrEqVal(data.getRoot(), ID, edit);
            edit(elem, key, value);
            return "";
        }

        // 2. Save changes
        if (save != null && gm) {
            data.save(null);
        }

        // 3. Standard view
        if (view != null) {
            final String relFullPath = view + "/index.xsl";
            return data.transformAllDataWithForeignNodes(Paths.get(".", relFullPath.split("/")));
        }

        // 4. Tagged elements only
        if (tag != null && tmpl != null) {
            return getTaggedView(tag, key, tmpl, gm);
        }

        return "";
    }

    /**
     * Edit specifics.
     * @param elem element to edit
     * @param key key that is being changed
     * @param value new value of the key
     * @throws Exception on error
     */
    private void edit(final Element elem, final String key, final String value) throws Exception {
        switch (key) {
        case PERMIT:
        case "select":
            elem.setAttribute(key, value);
            HttpServer.push(getPluginId(),
                    "id=" + elem.getAttribute(ID) + ":" + key + "=" + value
                            + ":name=" + elem.getAttribute("name"));
            data.save(elem);
            break;
        default:
            break;
        }
    }

    /**
     * Utility.
     * @param node node to check
     * @return permitted to read/modify?
     */
    public boolean permit(final Element node) {
        final String val = node.getAttribute(PERMIT);
        return !(val == null || val.length() == 0 || "false".equals(val));
    }

    /**
     * Get tag-filtered view of doc-tag.
     * @param tag element name
     * @param key names of the tags
     * @param template file for transform XSL
     * @param gm all or permit only
     * @return string output version
     * @throws Exception on error
     */
    private String getTaggedView(final String tag, final String key, final String template, final boolean gm) throws Exception {
        // Get elements with tagged keys
        final Node elemRoot = data.copyNodesByTagsAndKey(tag, key, gm);
        // Get all tags with tagged keys (which are also in the list)
        final Node tagRoot = data.createTagsByKey(key, gm, elemRoot.getChildNodes());
        return data.transformAllDataWithForeignNodes(Paths.get(".", template.split("/")), elemRoot, tagRoot);
    }
}
