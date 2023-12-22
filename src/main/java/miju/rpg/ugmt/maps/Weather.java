package miju.rpg.ugmt.maps;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to create the weather topology. Uses height as a raininess measure.
 */
public class Weather {
    /** Maps zooms to height field maps. */
    @SuppressWarnings("unchecked")
    private Map<String, Double>[] zoom2heights = new Map[32];

    /** The highest zoom level that is completely flat. */
    private int flatZoom = 1;

    /**
     * Constructor.
     * @param flatZoom highest zoom level to keep flat.
     */
    public Weather(int flatZoom) {
        this.flatZoom = flatZoom;
        for (int z = 0; z < flatZoom; z++) {
            zoom2heights[z] = Collections.synchronizedMap(new HashMap<>());
            for (int x = 0; x < 1 << z; x++) {
                for (int y = 0; y < 1 << z; y++) {
                    zoom2heights[z].put(x + "," + y, 0.);
                    zoom2heights[z].put(x + "," + y, 0.);
                    zoom2heights[z].put(x + "," + y, 0.);
                    zoom2heights[z].put(x + "," + y, 0.);
                }
            }
        }
    }

    /**
     * Generate an entry from the height field. Standard (recursive) fractal
     * algorithm. Coordinates will be cut to bounds.
     * @param zoom zoom level
     * @param x x coordinate (at zoom)
     * @param y y coordinate (at zoom)
     * @return height
     */
    synchronized public Double generate(int zoom, long x, long y) {
        if (zoom2heights[zoom] == null) {
            zoom2heights[zoom] = new HashMap<>();
        }
        x = mod(x, zoom);
        y = mod(y, zoom);
        Double ret = zoom2heights[zoom].get(x + "," + y);
        if (ret != null) {
            return ret;
        }
        if (x%2 == 0) {
            if (y%2 == 0) {
                Double p1 = generate(zoom - 1, x/2, y/2);
                zoom2heights[zoom].put(x + "," + y,  p1);
            }
            else {
                Double p1 = generate(zoom - 1, x/2, y/2);
                Double p2 = generate(zoom - 1, x/2, y/2 + 1);
                zoom2heights[zoom].put(x + "," + y,  (p1 + p2)/2. + random(zoom));
            }
        }
        else {
            if (y%2 == 0) {
                Double p1 = generate(zoom - 1, x/2, y/2);
                Double p2 = generate(zoom - 1, x/2 + 1, y/2);
                zoom2heights[zoom].put(x + "," + y,  (p1 + p2)/2. + random(zoom));
            }
            else {
                Double p1 = generate(zoom, x - 1, y - 1);
                Double p2 = generate(zoom, x + 1, y - 1);
                Double p3 = generate(zoom, x - 1, y + 1);
                Double p4 = generate(zoom, x + 1, y + 1);
                zoom2heights[zoom].put(x + "," + y,  (p1 + p2 + p3 + p4)/4. + random(zoom));
            }
        }
        ret = zoom2heights[zoom].get(x + "," + y);
        return ret;
    }

    /** Simple modulus. */
    private long mod(long val, int zoom) {
        if (val >= 1 << zoom) {
            return val - (1 << zoom);
        }
        if (val < 0) {
            return val + (1 << zoom);
        }
        return val;
    }

    /** Our random function. */
    private double random(int zoom) {
        return (2*Math.random() - 1) / Math.pow(2,  zoom - flatZoom);
    }
    
    /**
     * Get the image.
     * @param zoom zoom level
     * @param x x coordinate (at zoom)
     * @param y y coordinate (at zoom)
     * @return image for display
     */
    public BufferedImage getImage(int zoom, long x, long y) {
        BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                Double p = generate(zoom + 8, 256 * x + i, 256 * y + j);
                if (p <= 0)
                    bi.setRGB(i, j, 0x00000000);
                else if (p <= .5)
                    bi.setRGB(i, j, 0x80bbbbbb);
                else if (p <= .8)
                    bi.setRGB(i, j, 0xa0dddddd);
                else
                    bi.setRGB(i, j, 0xc0ffffff);
            }
        }
        return bi;
    }
}
