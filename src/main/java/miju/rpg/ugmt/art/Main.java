package miju.rpg.ugmt.art;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.w3c.dom.Element;

import miju.rpg.ugmt.AbstractMain;
import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.GraphicsUtilities;
import miju.rpg.ugmt.HttpQueryParams;
import miju.rpg.ugmt.XmlStreamsUtil;
import static miju.rpg.ugmt.XmlNames.Attributes.PERMIT;
import static miju.rpg.ugmt.XmlNames.Attributes.FILE;

/**
 * Main launch class.
 */
public class Main extends AbstractMain { // NO_UCD (unused code)
    @Override
    public String getContentType(final HttpQueryParams args) throws UnsupportedEncodingException {
        final String size = args.getSize();
        if (size != null) {
            return "image/png;";
        }
        return super.getContentType(args);
    }

    @Override
    public Object getContent(final HttpQueryParams args, final boolean gm) throws Exception {
        // Get parameters
        final String size = args.getSize();
        final String img = args.getImg();
        final String id = args.getId();

        // super default
        if (size == null || (id == null && img == null)) {
            return super.getContent(args, gm);
        }

        // XML element to get
        Element elem = null;
        if (id != null) {
            elem = getData().getElementById(id);
        }
        else { // image != null
            elem = XmlStreamsUtil.getElementByTagAndAttrEqVal(getData().getRoot(), "art", FILE, img);
        }

        // File to get (from element)
        final Path relPath;
        if (elem != null && (gm || "true".equals(elem.getAttribute(PERMIT)))) {
            relPath = Paths.get(".", elem.getAttribute(FILE).split("/"));
        }
        else {
            relPath = Paths.get(".", img.split("/"));
        }

        // Render image to byte stream
        if (relPath != null) {
            return renderImage(size, relPath);
        }

        return new byte[0];
    }

    /**
     * Render the image for the output stream. Scale according to "best fit", if
     * the image is larger than th requested size, else center. Images with 0
     * width or height or size = 0 will result in strange results.
     * @param strSize size of image
     * @param relPath file to load
     * @return output stream
     * @throws IOException on error
     */
    private Object renderImage(final String strSize, final Path relPath) throws IOException {
        // Draw scaled image
        final int size = Integer.parseInt(strSize);
        final BufferedImage image = readBestSizeImage(size, relPath);
        final int w = image.getWidth();
        final int h = image.getHeight();
        final BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = (Graphics2D) buf.getGraphics();
        if (w > h && size < w) {
            g.drawImage(GraphicsUtilities.getScaledInstance(image, size, (size * h) / w), 0, (size - (size * h) / w) / 2, null);
        }
        else /* w <= h || size >= w */ if (w <= h && size < h) {
            g.drawImage(GraphicsUtilities.getScaledInstance(image, (size * w) / h, size), (size - (size * w) / h) / 2, 0, null);
        }
        else { /* w <= h <= size || size >= w > h, i.e. size >= w && size >= h */
            g.drawImage(image, (size - w) / 2, (size - h) / 2, null);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buf, "png", baos);
        return baos.toByteArray();
    }

    /**
     * This checks, whether the image is already available for the desired size.
     * @param size desired size
     * @param relPath pure filename
     * @return desired image, hopefully of right size.
     * @throws IOException on error
     */
    private BufferedImage readBestSizeImage(final int size, final Path relPath) throws IOException {
        if (relPath != null && relPath.getParent() != null && relPath.getFileName() != null) {
            final Path tmp = relPath.getFileName();
            final Path best = relPath.getParent().resolve(tmp.toString() + "-" + size);
            final Path absFile = Data.ROOT_ABS_PATH.resolve(best);
            if (Files.exists(absFile)) {
                return ImageIO.read(absFile.toFile());
            }
            else {
                return ImageIO.read(Data.ROOT_ABS_PATH.resolve(relPath).toFile());
            }
        }
        return null;
    }
}
