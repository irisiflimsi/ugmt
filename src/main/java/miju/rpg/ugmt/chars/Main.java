package miju.rpg.ugmt.chars;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import miju.rpg.ugmt.AbstractMain;
import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.HttpQueryParams;

/**
 * Main launch class.
 */
public class Main extends AbstractMain { // NO_UCD (unused code)
    /** Helper string constant. */
    static final String MAP = "map";

    /** Helper string constant. */
    static final String OUT = "out";

    /** Helper string constant. */
    static final String PDF = "pdf";

    /** Helper constant. */
    private static final String CHARS_REL_PATH = "chars";

    /** Transforming the character into XML/SVG output. */
    private SimpleSheetTransformer trafo = new SimpleSheetTransformer();

    /** Transforming the character into PDF output. */
    private CharFop fop = new CharFop();

    @Override
    public String getContentType(final HttpQueryParams args) throws UnsupportedEncodingException {
        final String out = args.getValue(OUT, true);
        if (args.getValue(MAP, true) != null) {
            return "text/plain";
        }
        if (PDF.equals(out)) {
            return "application/pdf";
        }
        return super.getContentType(args);
    }

    @Override
    public Object getContent(final HttpQueryParams args, final boolean gm) throws Exception {
        final String id = args.getId();
        // This is a different template than "tmpl"
        final String template = args.getValue("template", true);
        final String out = args.getValue(OUT, true);
        if (PDF.equals(out)) {
            return getCharPrint(id, gm);
        }
        if (template != null) {
            return getCharXML(id, template, gm);
        }

        return super.getContent(args, gm);
    }

    /**
     * Provides the PDF from the individual page SVGs for a character.
     * @param id id of character
     * @param gm permit cascade
     * @return PDF from FOP
     * @throws Exception on error
     */
    private Object getCharPrint(final String id, final boolean gm) throws Exception {
        List<String> list = new ArrayList<>();
        for (int i = 1; true; i++) {
            String page = getCharXML(id, "char" + i + ".svg", gm);
            page = page.replaceAll("<\\?.*\\?>", "");
            if (page.length() == 0) {
                break;
            }
            File fi = File.createTempFile("ugmt2-char-" + i, ".svg");
            File fo = File.createTempFile("ugmt2-char-" + i, ".pdf");
            OutputStream is = new FileOutputStream(fi);
            is.write(page.getBytes(StandardCharsets.UTF_8));
            is.close();
            fop.transform(fi.getAbsolutePath(), fo.getAbsolutePath());
            list.add(fo.getAbsolutePath());
        }
        return fop.transform(list);
    }

    /**
     * Provides an XML string from a file modified for a character. Empty if no
     * file found.
     * @param id id of character
     * @param fileName (local) file name
     * @param gm permit cascade
     * @return XML string
     * @throws Exception on error
     */
    private String getCharXML(final String id, final String fileName, final boolean gm) throws Exception {
        // Get char
        final Element charNode = getData().getElementById(id);
        if (charNode == null || !(gm || permit(charNode))) {
            return "";
        }
        final String rules = charNode.getAttribute("rules");
        final CharProxy ch = CharProxy.getCharacterProxy(charNode);

        // Check rules specific and default template
        final Path absPath = Data.ROOT_ABS_PATH.resolve(Paths.get(CHARS_REL_PATH, rules, fileName));
        if (!Files.exists(absPath)) {
            return "";
        }

        return trafo.transform(ch, new String(Files.readAllBytes(absPath)));
    }
}

// *** getCharPrint: See FOP
//String template = new String(Files.readAllBytes(Data.ROOT_ABS_PATH.resolve(Paths.get(CHARS_REL_PATH, "pdf.fo"))));
//
//for (int i = 1; true; i++) {
//  String page = getCharXML(id, "char" + i + ".svg", gm);
//  page = page.replaceAll("<\\?.*\\?>", "");
//  template = template.replaceAll("\\$page" + i, page);
//  if (page.length() == 0) {
//      break;
//  }
//}
//
//template = template.replaceAll("width=\"100%\"", "width=\"209mm\"");
//template = template.replaceAll("height=\"auto\"", "height=\"296mm\"");
//return fop.transform(template);
