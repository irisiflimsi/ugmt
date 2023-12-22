package miju.rpg.ugmt;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Thread class that monitors file changes. Note that at least some file system
 * register with two events for a file change that is meant as "one" change.
 */
public final class Monitor extends Thread {
    /** Sleep, changing a file isn't (always?) an atomic operation. */
    private static final int SLEEP = 100;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

    /** Object to inform about changes in this thread. */
    private final AbstractXmlProvider xmlProvider;

    /** Java watch object. */
    private final WatchService watchService;

    /** Path watched. */
    private final Path absWatchPath;

    /**
     * Constructor.
     * @param anAbsWatchPath path to monitor
     * @param anXmlProvider where data will be integrated
     * @throws IOException I/O error
     */
    private Monitor(final AbstractXmlProvider anXmlProvider, final Path anAbsWatchPath) throws IOException {
        this.xmlProvider = anXmlProvider;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.absWatchPath = anAbsWatchPath;
        absWatchPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /**
     * Monitor file changes.
     * @param absPath directory to monitor
     * @param anXmlProvider where data will be integrated
     * @throws IOException on file problems
     */
    public static void monitor(final AbstractXmlProvider anXmlProvider, final Path absPath) throws IOException {
        LOGGER.info("watch={}", absPath);
        new Monitor(anXmlProvider, absPath).start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                watch();
            }
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
    }

    /**
     * Watch the file system for changes and if a change happens, load the new
     * files.
     * @throws InterruptedException on error
     * @throws IOException on error
     * @throws SAXException on error
     */
    private void watch() throws InterruptedException, SAXException, IOException {
        final WatchKey wk = watchService.take();
        // This is to coerce file change and meta change events
        Thread.sleep(SLEEP);
        for (WatchEvent<?> event : wk.pollEvents()) {
            final Path relChangedPath = (Path) event.context();
            if (relChangedPath != null && relChangedPath.toString().endsWith(".xml")) {
                LOGGER.warn("changedPath={}", relChangedPath);
                xmlProvider.load(absWatchPath.resolve(relChangedPath));
            }
        }
        // reset the key
        wk.reset();
    }
}
