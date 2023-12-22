package miju.rpg.ugmt.chars;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import miju.rpg.ugmt.Data;

/**
 * Obvious FOP managing class.
 */
class CharFop {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CharFop.class);

    /**
     * Transform.
     * @param string string to transform
     * @return transformed string
     * @throws Exception on error
     */

    synchronized byte[] transform(final String fileIn, final String fileOut) {
        try {
            final String[] params = {"inkscape", fileIn, "--without-gui", "--export-pdf=" + fileOut };
            LOGGER.info("start: {} {} {} {}", params[0], params[1], params[2], params[3]);
            ProcessBuilder pb = new ProcessBuilder(params[0], params[1], params[2], params[3]);
            pb.directory(Data.ROOT_ABS_PATH.toFile());
            Process p = pb.start();
            p.waitFor();
            LOGGER.info("ended: {} {} {} {}", params[0], params[1], params[2], params[3]);
            return null;
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
        return null;
    }

    synchronized byte[] transform(List<String> fileNames) {
        try {
            File f = File.createTempFile("ugmt2-char", ".pdf");
            final String[] params = {"pdftk", fileNames.get(0), fileNames.get(1), fileNames.get(2), "cat", "output", f.getAbsolutePath() };
            LOGGER.info("start: {} {} {} {} {} {} {}", params[0], params[1], params[2], params[3], params[4], params[5], params[6]);
            ProcessBuilder pb = new ProcessBuilder(params[0], params[1], params[2], params[3], params[4], params[5], params[6]);
            pb.directory(Data.ROOT_ABS_PATH.toFile());
            Process p = pb.start();
            p.waitFor();
            LOGGER.info("ended: {} {} {} {} {} {} {}", params[0], params[1], params[2], params[3], params[4], params[5], params[6]);
            return Files.readAllBytes(Paths.get(f.getAbsolutePath()));
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
        return null;
    }

// *** FOP has a serious problem with fonts I can't resolve in an adequat amount of time. Dunmp Fop, sorry.

//     import org.apache.avalon.framework.configuration.Configuration;
//     import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
//     import org.apache.fop.apps.Fop;
//     import org.apache.fop.apps.FopFactory;
//     import org.apache.fop.apps.FopFactoryBuilder;
//     import org.apache.fop.apps.MimeConstants;

//    final DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
//    final Configuration cfg = cfgBuilder.buildFromFile(new File(System.getProperty("FOP")));
//    final FopFactory fopFactory = new FopFactoryBuilder(Data.ROOT_ABS_PATH.toUri()).setConfiguration(cfg).build();
//    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
//    final StreamResult streamResult = new StreamResult(bos);
//    final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, streamResult.getOutputStream());
//    final Transformer transformer = TransformerFactory.newInstance().newTransformer();
//    final Result result = new SAXResult(fop.getDefaultHandler());
//    final Source source = new StreamSource(new StringReader(string));
//    transformer.transform(source, result);
//    return bos.toByteArray();


}
