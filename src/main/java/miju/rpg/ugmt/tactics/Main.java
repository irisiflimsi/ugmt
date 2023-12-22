package miju.rpg.ugmt.tactics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import miju.rpg.ugmt.AbstractMain;
import miju.rpg.ugmt.Data;
import miju.rpg.ugmt.HttpQueryParams;
import miju.rpg.ugmt.HttpServer;
import static miju.rpg.ugmt.XmlNames.Attributes.PERMIT;
import static miju.rpg.ugmt.XmlNames.Attributes.FILE;
import static miju.rpg.ugmt.XmlNames.Attributes.ID;

/**
 * Main launch class.
 */
public class Main extends AbstractMain { // NO_UCD (unused code)
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /** Helper string constant. */
    private static final String PNG = "png";

    /** Helper string constant. */
    private static final Path CACHE_REL_PATH = Paths.get("cache");

    /** Empty default image. */
    private static final BufferedImage EMPTY = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

    @Override
    public String getContentType(final HttpQueryParams args) throws UnsupportedEncodingException {
        if (args.getSet() != null) {
            return "text/plain;";
        }
        if (args.getId() != null) {
            return "image/png;";
        }
        return super.getContentType(args);
    }

    @Override
    public Object getContent(final HttpQueryParams args, final boolean gm) throws Exception {
        final String set = args.getSet();
        String mapId = "";
        if (set != null) {
            mapId = set;
            HttpServer.push(getPluginId(), ID + "=" + mapId + ":set=true");
            return "";
        }

        final String get = args.getValue("get", true);
        if (get != null) {
            return mapId;
        }

        final String id = args.getId();
        final String x = args.getValue("x", true);
        final String y = args.getValue("y", true);
        final String scale = args.getValue("scale", true);

        if (id != null) {
            final BufferedImage ret = getCachedImage(id, Integer.parseInt(x),
                    Integer.parseInt(y), Double.parseDouble(scale), gm);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(ret, PNG, baos);
            return baos.toByteArray();
        }

        return super.getContent(args, gm);
    }

    /**
     * This method allows for the GM to view the map and a player may then also
     * see it. But that implies hacking already, since the tagged list does not
     * show the image. TODO scale
     * @param id id of map
     * @param x x coordinate on map
     * @param y y coordinate of map
     * @param scale scale of map requested
     * @param gm GM query?
     * @return Image containing the (tile of the) map requested
     * @throws IOException in case of error
     */
    private BufferedImage getCachedImage(final String id, final int x, final int y, final double scale, final boolean gm) throws IOException {
        final Path absDirPath = Data.ROOT_ABS_PATH.resolve(CACHE_REL_PATH.resolve(id));
        if (!Files.exists(absDirPath)) {
            Files.createDirectory(absDirPath);
        }
        final Path absFilePath = Data.ROOT_ABS_PATH.resolve(makeCacheName(id, x, y, scale));
        if (Files.exists(absFilePath)) {
            return ImageIO.read(absFilePath.toFile());
        }

        final String tiles = getData().getElementById(id).getAttribute("tiles");
        final String fname = getData().getElementById(id).getAttribute(FILE);
        final String permit = getData().getElementById(id).getAttribute(PERMIT);

        if (gm || "true".equals(permit)) {
            final BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            if (tiles != null && !"".equals(tiles)) {
                final Path relTileFile = buildTileFileName(x, y, tiles, fname);
                LOGGER.debug("relTileFile={}", relTileFile);
                final BufferedImage orig = ImageIO.read(Data.ROOT_ABS_PATH.resolve(relTileFile).toFile());
                bi.getGraphics().drawImage(orig, 0, 0, null);
                ImageIO.write(bi, PNG, absFilePath.toFile());
            }
            else {
                final BufferedImage orig = ImageIO.read(absFilePath.toFile());
                bi.getGraphics().drawImage(orig, -x, -y, null);
                ImageIO.write(bi, PNG, absFilePath.toFile());
            }
            return bi;
        }
        return EMPTY;
    }

    /**
     * Build a cache tile (relative) file name.
     * @param x x coordinate in overall map.
     * @param y y coordinate in overall map.
     * @param tile tile of map requested
     * @param fileName map's file name
     * @return tile (cache) name
     */
    private Path buildTileFileName(final int x, final int y, final String tile, final String fileName) {
        final int li = fileName.lastIndexOf('/') + 1;
        return Paths.get(fileName.substring(0, li) + tile, y + "_" + x + "_" + fileName.substring(li));
    }

    /**
     * Build a cache tile (relative) file name.
     * @param x x coordinate in overall map.
     * @param y y coordinate in overall map.
     * @param id overall map id
     * @param scale current scale of map
     * @return cache name
     */
    private Path makeCacheName(final String id, final int x, final int y, final double scale) {
        return CACHE_REL_PATH.resolve(Paths.get(id, x + "_" + y + "_" + scale + ".png"));
    }
}
