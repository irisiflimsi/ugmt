package miju.rpg.ugmt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is used to asynchronously answer a HTTP request.
 */
public class HttpWorkerThread implements Runnable {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpWorkerThread.class);

    /** Helper constant. */
    private static final String WEBSOCKET_INFIX = "/socket/";

    /** Helper constant. */
    private static final String HTTP_CONTENTTYPE = "Content-Type: ";

    /** Helper constant. */
    private static final String HTTP_LF = "\r\n";

    /** Helper constant. */
    private static final String HTTP_OK = "HTTP/1.1 200 OK";

    /** Socket for communication. */
    private final SocketChannel socketChannel;

    /** Is this the GM (or a player) socket? */
    private final boolean gm;

    /**
     * Constructor.
     * @param aSocketChannel socket for communication.
     * @param aGm GM's socket?
     */
    HttpWorkerThread(final SocketChannel aSocketChannel, final boolean aGm) {
        socketChannel = aSocketChannel;
        gm = aGm;
    }

    /**
     * Reads from socket and writes to socket. The socket should provide
     * something or this thread wouldn't have been spawned.
     */
    @Override
    public void run() {
        try {
            final Socket socket = socketChannel.socket();
            final BufferedReader bsr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // HTTP GET line
            String in = bsr.readLine();
            if (in == null) {
                in = "";
            }
            in = URLDecoder.decode(in, StandardCharsets.UTF_8.toString());
            LOGGER.info("in={}", in);
            final String[] requestParams = in.split("  *");
            final boolean head = requestParams[0].equals("HEAD");

            try (OutputStream os = socket.getOutputStream()) {
                if (requestParams.length < 2) {
                    LOGGER.warn("Query deprecated!");
                    return;
                }

                // Handle websockets
                String query = requestParams[1].substring(1);
                if (requestParams[1].startsWith(WEBSOCKET_INFIX)) {
                    handleWebSocket(requestParams[1].substring(WEBSOCKET_INFIX.length()), bsr, os);
                    return;
                }

                // Get standard query
                MimeType foundMimeType = null;
                for (MimeType mimeType : MimeType.values()) {
                    if (query.endsWith(mimeType.getExtension()) || query.indexOf(mimeType.getExtension() + "?") > -1) {
                        foundMimeType = mimeType;
                        break;
                    }
                }

                // Look for file
                query = query.replaceAll("http://.*?/*", "");
                Path absPath = Data.string2file(query.replace("favicon.ico","favicon.png"));
                if (!Files.exists(absPath) || !Files.isReadable(absPath)) {
                    foundMimeType = null;
                }

                // Directory? => index.html
                if (Files.isDirectory(absPath)) {
                    final Path absIdxHtml = absPath.resolve("index.html");
                    if (Files.exists(absIdxHtml) && Files.isReadable(absIdxHtml)) {
                        foundMimeType = MimeType.HTML;
                        absPath = absIdxHtml;
                    }
                }

                try (PrintWriter osr = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {
                    if (gm && foundMimeType == null && Files.isDirectory(absPath) && Files.isReadable(absPath)) {
                        // Directory listing
                        handleDirectory(head, absPath, osr);
                    }
                    else if (foundMimeType != null) {
                        // Standard query
                        handleStandardQuery(head, foundMimeType.getMimeType(), absPath, osr);
                    }
                    else {
                        final String[] args = query.split("\\?|&|=");
                        final String clsName = "miju.rpg." + args[0].replaceAll(".*(ugmt.*)", "$1") + ".Main";
                        final String clsRsrcName = clsName.replaceAll("\\.", "/") + ".class";
                        LOGGER.debug("query={} clsRsrcName={}", query, clsRsrcName);
                        if (HttpServer.class.getClassLoader().getResource(clsRsrcName) != null) {
                            // Programmatic
                            handleProgrammatic(clsName, args, head, os, osr);
                        }
                        else {
                            // Error
                            handleError(head, osr);
                        }
                    }
                }
            }
        }
        catch (final Exception e) {
            LOGGER.error("", e);
        }
    }

    /**
     * Handle error output.
     * @param head handling HEAD directive
     * @param osr print writer
     * @throws Exception on error
     */
    private void handleError(final boolean head, final PrintWriter osr) throws Exception {
        osr.println("HTTP/1.1 403 FORBIDDEN");
        osr.println();
        osr.flush();
        if (!head) {
            final FileInputStream fileInputStream = new FileInputStream(Data.string2file("error.html").toFile());
            final FileChannel fc = fileInputStream.getChannel();
            fc.transferTo(0, fc.size(), socketChannel);
            fileInputStream.close();
        }
    }

    /**
     * Handle program output.
     * @param cls class to start
     * @param args GET query string split
     * @param head HEAD directive?
     * @param os output stream
     * @param osr print writer
     * @throws Exception on error
     */
    private void handleProgrammatic(final String cls, final String[] args, final boolean head, final OutputStream os, final PrintWriter osr) throws Exception {
        LOGGER.debug("cls={}", cls);
        final HttpQueryParams argv = new HttpQueryParams(args);
        final AbstractMain launch = HttpServer.launch(cls);

        writeHeader(launch.getContentType(argv), osr);
        if (!head) {
            final Object out = launch.getContent(argv, gm);
            LOGGER.debug("out.class={}", out.getClass());
            if (out instanceof String) {
                osr.println(out);
            }
            else if (out instanceof File) {
                final FileInputStream fileInputStream = new FileInputStream((File)out);
                final FileChannel fc = fileInputStream.getChannel();
                fc.transferTo(0, fc.size(), socketChannel);
                fileInputStream.close();
            }
            else { // byte[]
                os.write((byte[]) out);
                os.flush();
            }
        }
    }

    /**
     * Handle standard query output.
     * @param head handling HEAD directive
     * @param type file type content-type
     * @param absPath file to serve
     * @param osr print writer
     * @throws Exception on error
     */
    private void handleStandardQuery(final boolean head, final String type, final Path absPath, final PrintWriter osr) throws Exception {
        writeHeader(type, osr);
        if (!head) {
            final FileInputStream fileInputStream = new FileInputStream(absPath.toFile());
            final FileChannel fc = fileInputStream.getChannel();
            fc.transferTo(0, fc.size(), socketChannel);
            fileInputStream.close();
        }
    }

    /**
     * Handle directory listing.
     * @param head handling HEAD directive
     * @param absPath directory to serve
     * @param osr print writer
     * @throws Exception on error
     */
    private static void handleDirectory(final boolean head, final Path absPath, final PrintWriter osr) throws Exception {
        writeHeader(MimeType.HTML.getMimeType(), osr);
        if (!head) {
            osr.println(showDir(absPath));
        }
    }

    /**
     * Utility to turn directory listing into XML.
     * @param absPath directory to show
     * @return XSL transformed directory listing
     * @throws Exception XSL problems
     */
    private static String showDir(final Path absPath) throws Exception {
        final Document doc = AbstractXmlProvider.newDocument();
        final Element root = doc.createElement("root");
        doc.appendChild(root);

        final DirectoryStream<Path> stream = Files.newDirectoryStream(absPath);
        for (Path entry : stream) {
            final Element el = doc.createElement("file");
            final Path pathName = absPath.getFileName();
            final Path entryName = entry.getFileName();
            if (pathName != null && entryName != null) {
                el.setAttribute("path", pathName.toString() + "/" + entryName.toString());
            }
            if (entryName != null) {
                el.setAttribute("name", entryName.toString());
            }
            root.appendChild(el);
        }
        return AbstractXmlProvider.transform(doc, Paths.get("dir.xsl"));
    }

    /**
     * Handle header.
     * @param type content type
     * @param osr stream to write to
     */
    private static void writeHeader(final String type, final PrintWriter osr) {
        osr.println(HTTP_OK);
        osr.println(HTTP_CONTENTTYPE + type);
        osr.println();
        osr.flush();
    }

    /**
     * Handle web-socket register query.
     * @param plugin plugin to handle socket for
     * @param bsr reader to read the rest of the protocol
     * @param os output stream
     * @throws Exception on error
     */
    private static void handleWebSocket(final String plugin, final BufferedReader bsr, final OutputStream os) throws Exception {
        final PrintWriter osr = new PrintWriter(os, false);
        // Handshake
        String key = null;
        while (key == null) {
            final String data = bsr.readLine();
            LOGGER.info("data={}", data);
            if (data != null && data.startsWith("Sec-WebSocket-Key")) {
                final String[] keys = data.split(": ");
                key = keys[1] + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                key = getSHA1(key);
            }
        }
        osr.print("HTTP/1.1 101 Switching Protocols" + HTTP_LF);
        osr.print("Upgrade: websocket\r\n");
        osr.print("Connection: Upgrade\r\n");
        osr.print("Sec-WebSocket-Accept: " + key + HTTP_LF);
        osr.print(HTTP_LF);
        osr.flush();
        HttpServer.addWebSocket(plugin, os);
    }

    /**
     * Get SHA1 digest for text.
     * @param text text to digest
     * @return digest
     * @throws NoSuchAlgorithmException on error
     */
    private static String getSHA1(final String text) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        final byte[] orig = text.getBytes(StandardCharsets.UTF_8);
        md.update(orig, 0, orig.length);
        final byte[] sha1hash = md.digest();
        return DatatypeConverter.printBase64Binary(sha1hash);
    }
}
