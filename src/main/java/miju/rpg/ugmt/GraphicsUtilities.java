package miju.rpg.ugmt;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 * Copied from
 * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html.
 * Slightly modified.
 */
public final class GraphicsUtilities {
    /**
     * Hide constructor.
     */
    private GraphicsUtilities() {
    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code BufferedImage}.
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(final BufferedImage img, final int targetWidth, final int targetHeight) {
        int type = BufferedImage.TYPE_INT_ARGB;
        if (img.getTransparency() == Transparency.OPAQUE) {
            type = BufferedImage.TYPE_INT_RGB;
        }
        BufferedImage ret = img;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        int w = img.getWidth();
        int h = img.getHeight();

        do {
            if (w > targetWidth) {
                w /= 2;
            }
            if (w < targetWidth) {
                w = targetWidth;
            }

            if (h > targetHeight) {
                h /= 2;
            }
            if (h < targetHeight) {
                h = targetHeight;
            }

            final BufferedImage tmp = new BufferedImage(w, h, type);
            final Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        }
        while (w != targetWidth || h != targetHeight);
        return ret;
    }
}
