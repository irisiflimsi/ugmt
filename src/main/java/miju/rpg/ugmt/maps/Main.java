package miju.rpg.ugmt.maps;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import miju.rpg.ugmt.AbstractMain;
import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.GraphicsUtilities;
import miju.rpg.ugmt.HttpQueryParams;
import miju.rpg.ugmt.MimeType;
import miju.rpg.ugmt.XmlStreamsUtil;

/**
 * Main launch class.
 */
public class Main extends AbstractMain { // NO_UCD (unused code)
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final String ROOT = "/storage/regional/";
    
    private static Weather weather = new Weather(4);

    @Override
    public String getContentType(final HttpQueryParams args) throws UnsupportedEncodingException {
        String f = args.getValue("f", false);
        if (f != null) {
            return "image/" + f;
        }
        return MimeType.HTML.getMimeType();
    }

    @Override
    public Object getContent(final HttpQueryParams args, final boolean gm) throws Exception {
        String f = args.getValue("f", false);
        String zs = args.getValue("z", false);
        if (zs != null) {
            if (f == null) {
                f = "png";
            }
            int z = Integer.parseInt(zs);
            int x = Integer.parseInt(args.getValue("x", false));
            int y = Integer.parseInt(args.getValue("y", false));
            if (f.equals("wth")) {
                File file = File.createTempFile("wth", "png");
                ImageIO.write(weather.getImage(z, x, y), "png", file);
                return file;
            }
            File file = new File(ROOT + z + "/" + x + "/" + y + "." + f);
            LOGGER.info("file={} exists= {}", file.getAbsolutePath(), file.exists());
            if (!file.exists()) {
                 FileUtils.writeByteArrayToFile(file, getBytes(z,x,y,f));
            }
            return file;
        }
        return super.getContent(args, gm);
    }

    /**
     * Delivers a byte array of image data for our map in coordinates that leaflet.js uses.
     * Can be cached.
     * @param z zoom level
     * @param x x coordinate as per leaflet.js
     * @param y y coordinate as per leaflet.js
     * @return
     * @throws IOException 
     */
    private byte[] getBytes(int z, int x, int y, String f) throws IOException {
        int lx = x;
        int ly = y;
        int lz = z;
        int scale = 1;
        File file = new File(ROOT + lz + "/" + lx + "/" + ly + "." + f);
        while (!file.exists()) {
            lz--;
            lx /= 2;
            ly /= 2;
            file = new File(ROOT + lz + "/" + lx + "/" + ly + "." + f);
            scale *= 2;
        }
        LOGGER.info("lx={}, ly={}, scale={}, file={} exists={}", lx, ly, scale, file.getAbsolutePath(), file.exists());
        BufferedImage img = ImageIO.read(file);
        int upscale = scale * 256;
        int x0 = 256 * (x - scale * lx);
        int y0 = 256 * (y - scale * ly);
        BufferedImage scaledImg = GraphicsUtilities.getScaledInstance(img, upscale, upscale);
        LOGGER.info("x0={}, y0={}, upscale={}", x0, y0, upscale);
        BufferedImage subImg = scaledImg.getSubimage(x0, y0, 256, 256);

        List<Element> mapsAtScale = XmlStreamsUtil.getElementsByTagAndAttrEqVal(getData().getRoot(), "map", "scalef", Integer.toString(z));
        Graphics2D g2d = subImg.createGraphics();

        for (Element map : mapsAtScale) {
            double px = Double.parseDouble(map.getAttribute("x"));
            double py = Double.parseDouble(map.getAttribute("y"));
            BufferedImage partImg = ImageIO.read(Data.ROOT_ABS_PATH.resolve(map.getAttribute("file")).toFile());
            int pw = partImg.getWidth();
            int ph = partImg.getHeight();
            LOGGER.info("px={}, py={}, pw={}, ph={}", px, py, pw, ph);
            if ((px < x + 1) && (px + pw / 256.0 > x)
                    && (py < y + 1) && (py + ph / 256.0 > y)) {
                int ix = (int) ((px - x) * 256);
                int iy = (int) ((py - y) * 256);
                g2d.drawImage(partImg, ix, iy, null);
            }
        }
        g2d.dispose();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(subImg, f, baos);
        return baos.toByteArray();
    }
}
