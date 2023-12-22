package miju.rpg.ugmt;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class that controls the socket. It listens and pipes out appropriate HTTP
 * answers.
 */
public final class HttpServer {
    /** listener port. */
    private static final int PLPORT = 8080;

    /** listener port. */
    private static final int GMPORT = 8888;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    /** Main classes instantiated. */
    private static Map<String, AbstractMain> mainInstances = new Hashtable<String, AbstractMain>();

    /** All fixed XML/DOM data. */
    private static Data data;

    /** All connected clients. */
    private static Map<String, Set<OutputStream>> clients = new Hashtable<String, Set<OutputStream>>();

    /**
     * Hide Constructor.
     */
    private HttpServer() {
    }

    /**
     * Root method.
     * @param args argv
     * @exception Exception on error
     */
    public static void main(final String[] args) throws Exception {
        System.getProperties().load(HttpServer.class.getClassLoader().getResourceAsStream("appl.properties"));

        // Load data
        data = new Data();

        // Start GM socket
        final Thread gmthread = new SocketThread(GMPORT);
        gmthread.start();

        // Start player socket
        final Thread plthread = new SocketThread(PLPORT);
        plthread.start();
    }

    /**
     * This is for the web-sockets listeners. Protocol is basically free form.
     * @param plugin plugin that issues the message
     * @param txt the (raw) message issued.
     */
    public static void push(final String plugin, final String txt) {
        synchronized (clients) {
            final Set<OutputStream> plugs = clients.get(plugin);
            if (plugs != null) {
                LOGGER.debug("PUT plugin={} plugs.size={}: txt={}", plugin, plugs.size(), txt);
            }
            else {
                LOGGER.info("PUT plugin={} plugs.size=-1: txt={}", plugin, txt);
            }
            if (plugs == null) {
                return;
            }
            final Set<OutputStream> dead = new HashSet<OutputStream>();

            for (OutputStream client : clients.get(plugin)) {
                try {
                    client.write(encodeForWebSocket(txt));
                    client.flush();
                }
                catch (final Exception e) {
                    dead.add(client);
                    LOGGER.error("client={} declared dead, because of e={}", client, e);
                }
            }
            clients.get(plugin).removeAll(dead);
        }
    }

    /**
     * Add another web socket (stream).
     * @param plugin client registered for this plugin
     * @param os output stream to use
     */
    static synchronized void addWebSocket(final String plugin, final OutputStream os) {
        Set<OutputStream> streams = clients.get(plugin);
        if (streams == null) {
            streams = new HashSet<OutputStream>();
            clients.put(plugin, streams);
        }
        streams.add(os);
    }

    /**
     * Launch a main class.
     * @param cls class name
     * @return launched instance
     * @throws Exception on error
     */
    static synchronized AbstractMain launch(final String cls) throws Exception {
        AbstractMain launch = mainInstances.get(cls);
        if (launch == null) {
            launch = (AbstractMain) Class.forName(cls).getDeclaredConstructor().newInstance();
            mainInstances.put(cls, launch);
            launch.setData(data);
        }
        return launch;
    }

    /**
     * Encode String for web-sockets.
     * @param param string to encode
     * @return encoded string
     * @throws UnsupportedEncodingException on error
     */
    private static byte[] encodeForWebSocket(final String param) throws UnsupportedEncodingException {
        final byte[] pay = param.getBytes(StandardCharsets.UTF_8);
        byte[] head = new byte[] {
            (byte) (128 + 1), (byte) pay.length
        };
        if (pay.length > 256 * 256 - 1) {
            head = new byte[] {
                    (byte) (128 + 1), (byte) (128 - 1),
                    (byte) (pay.length / 256 / 256 / 256),
                    (byte) ((pay.length / 256 / 256) % 256),
                    (byte) ((pay.length / 256) % 256),
                    (byte) (pay.length % 256)
            };
        }
        else if (pay.length > 128 - 3) {
            head = new byte[] {
                    (byte) (128 + 1), (byte) (128 - 2),
                    (byte) (pay.length / 256), (byte) (pay.length % 256) };
        }
        final byte[] result = new byte[head.length + pay.length];
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(pay, 0, result, head.length, pay.length);
        return result;
    }

    /** Socket listener thread. */
    private static class SocketThread extends Thread {
        /** Port. */
        private int port;
        /** Thread pool. */
        private ExecutorService pool = Executors.newFixedThreadPool(10);

        /**
         * Constructor.
         * @param listen port to listen to
         */
        SocketThread(final int listen) {
            this.port = listen;
        }

        @Override
        public void run() {
            try {
                // Create two non-blocking server socket channel
                final ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(port));
                SocketChannel sc;
                while ((sc = ssc.accept()) != null) {
                    pool.execute(new HttpWorkerThread(sc, true));
                }
            }
            catch (final Exception e) {
                LOGGER.error("", e);
            }
        }
    }
}
