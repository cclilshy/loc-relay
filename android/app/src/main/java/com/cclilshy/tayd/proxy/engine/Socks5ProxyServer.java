package com.cclilshy.tayd.proxy.engine;

import com.cclilshy.tayd.proxy.domain.ProxyAuth;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class Socks5ProxyServer implements AutoCloseable {
    public interface LogSink {
        void append(String line);
    }

    private final InetAddress bindAddress;
    private final int requestedPort;
    private final ProxyAuth auth;
    private final LogSink logSink;
    private final ExecutorService workers;
    private ServerSocket server;
    private Thread acceptThread;

    public Socks5ProxyServer(InetAddress bindAddress, int port) {
        this(bindAddress, port, ProxyAuth.disabled(), null);
    }

    public Socks5ProxyServer(InetAddress bindAddress, int port, ProxyAuth auth) {
        this(bindAddress, port, auth, null);
    }

    public Socks5ProxyServer(InetAddress bindAddress, int port, ProxyAuth auth, LogSink logSink) {
        this.bindAddress = bindAddress;
        this.requestedPort = port;
        this.auth = auth == null ? ProxyAuth.disabled() : auth;
        this.logSink = logSink;
        this.workers = Executors.newCachedThreadPool(new NamedThreadFactory("socks5-proxy"));
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        server = new ServerSocket();
        server.bind(new InetSocketAddress(bindAddress, requestedPort));
        acceptThread = new Thread(this::acceptLoop, "socks5-proxy-accept");
        acceptThread.start();
        log("listening on " + bindAddress.getHostAddress() + ":" + server.getLocalPort()
                + (auth.isRequired() ? " with auth" : ""));
    }

    public synchronized int getPort() {
        if (server == null) {
            throw new IllegalStateException("server is not started");
        }
        return server.getLocalPort();
    }

    @Override
    public synchronized void close() {
        if (server != null) {
            SocketTunnel.closeQuietly(server);
            server = null;
        }
        workers.shutdownNow();
    }

    private void acceptLoop() {
        while (true) {
            ServerSocket current = server;
            if (current == null || current.isClosed()) {
                return;
            }
            try {
                Socket client = current.accept();
                workers.submit(() -> handle(client));
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private void handle(Socket client) {
        try (Socket socket = client) {
            socket.setTcpNoDelay(true);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            if (readByte(in) != 0x05) {
                return;
            }
            int methodCount = readByte(in);
            boolean supportsNoAuth = false;
            boolean supportsUserPass = false;
            for (int i = 0; i < methodCount; i++) {
                int method = readByte(in);
                if (method == 0x00) {
                    supportsNoAuth = true;
                } else if (method == 0x02) {
                    supportsUserPass = true;
                }
            }
            if (auth.isRequired()) {
                if (!supportsUserPass) {
                    out.write(new byte[] {0x05, (byte) 0xff});
                    out.flush();
                    log("rejected client: username/password auth required");
                    return;
                }
                out.write(new byte[] {0x05, 0x02});
                out.flush();
                if (!authenticate(in, out)) {
                    log("rejected client: invalid username/password");
                    return;
                }
            } else {
                if (!supportsNoAuth) {
                    out.write(new byte[] {0x05, (byte) 0xff});
                    out.flush();
                    return;
                }
                out.write(new byte[] {0x05, 0x00});
                out.flush();
            }

            if (readByte(in) != 0x05) {
                return;
            }
            int command = readByte(in);
            readByte(in);
            int addressType = readByte(in);
            if (command != 0x01) {
                out.write(new byte[] {0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
                out.flush();
                return;
            }

            String host = readHost(in, addressType);
            int port = readPort(in);
            Socket upstream = new Socket();
            try {
                upstream.connect(new InetSocketAddress(host, port), 15000);
                out.write(new byte[] {0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
                out.flush();
                log("CONNECT " + host + ":" + port);
                SocketTunnel.bridge(socket, upstream, workers);
            } catch (IOException err) {
                SocketTunnel.closeQuietly(upstream);
                out.write(new byte[] {0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
                out.flush();
                log("CONNECT " + host + ":" + port + " failed: " + err.getMessage());
            }
        } catch (IOException ignored) {
        }
    }

    private boolean authenticate(InputStream in, OutputStream out) throws IOException {
        if (readByte(in) != 0x01) {
            out.write(new byte[] {0x01, 0x01});
            out.flush();
            return false;
        }
        String username = new String(readExactly(in, readByte(in)), StandardCharsets.US_ASCII);
        String password = new String(readExactly(in, readByte(in)), StandardCharsets.US_ASCII);
        boolean accepted = auth.matches(username, password);
        out.write(new byte[] {0x01, accepted ? (byte) 0x00 : (byte) 0x01});
        out.flush();
        return accepted;
    }

    private static String readHost(InputStream in, int addressType) throws IOException {
        if (addressType == 0x01) {
            byte[] data = readExactly(in, 4);
            return InetAddress.getByAddress(data).getHostAddress();
        }
        if (addressType == 0x03) {
            int length = readByte(in);
            return new String(readExactly(in, length), StandardCharsets.US_ASCII);
        }
        if (addressType == 0x04) {
            byte[] data = readExactly(in, 16);
            return InetAddress.getByAddress(data).getHostAddress();
        }
        throw new IOException("unsupported address type: " + addressType);
    }

    private static int readPort(InputStream in) throws IOException {
        return (readByte(in) << 8) | readByte(in);
    }

    private static int readByte(InputStream in) throws IOException {
        int value = in.read();
        if (value == -1) {
            throw new EOFException();
        }
        return value & 0xff;
    }

    private static byte[] readExactly(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(data, offset, length - offset);
            if (read == -1) {
                throw new EOFException();
            }
            offset += read;
        }
        return data;
    }

    private void log(String line) {
        if (logSink != null) {
            logSink.append(line);
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private int nextId = 1;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + nextId++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
