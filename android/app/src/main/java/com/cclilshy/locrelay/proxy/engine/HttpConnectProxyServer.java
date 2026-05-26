package com.cclilshy.locrelay.proxy.engine;

import com.cclilshy.locrelay.proxy.domain.ProxyAuth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class HttpConnectProxyServer implements AutoCloseable {
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

    public HttpConnectProxyServer(InetAddress bindAddress, int port) {
        this(bindAddress, port, ProxyAuth.disabled(), null);
    }

    public HttpConnectProxyServer(InetAddress bindAddress, int port, ProxyAuth auth) {
        this(bindAddress, port, auth, null);
    }

    public HttpConnectProxyServer(InetAddress bindAddress, int port, ProxyAuth auth, LogSink logSink) {
        this.bindAddress = bindAddress;
        this.requestedPort = port;
        this.auth = auth == null ? ProxyAuth.disabled() : auth;
        this.logSink = logSink;
        this.workers = Executors.newCachedThreadPool(new NamedThreadFactory("http-proxy"));
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        server = new ServerSocket();
        server.bind(new InetSocketAddress(bindAddress, requestedPort));
        acceptThread = new Thread(this::acceptLoop, "http-proxy-accept");
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.trim().isEmpty()) {
                return;
            }
            String proxyAuthorization = null;
            while (true) {
                String header = reader.readLine();
                if (header == null || header.isEmpty()) {
                    break;
                }
                int colon = header.indexOf(':');
                if (colon > 0 && "Proxy-Authorization".equalsIgnoreCase(header.substring(0, colon).trim())) {
                    proxyAuthorization = header.substring(colon + 1).trim();
                }
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !"CONNECT".equalsIgnoreCase(parts[0])) {
                writeResponse(socket.getOutputStream(), "405 Method Not Allowed");
                return;
            }
            if (!isAuthorized(proxyAuthorization)) {
                writeProxyAuthRequired(socket.getOutputStream());
                log("rejected CONNECT " + parts[1] + ": auth required");
                return;
            }

            HostPort target = HostPort.parse(parts[1], 443);
            Socket upstream = new Socket();
            try {
                upstream.connect(new InetSocketAddress(target.host, target.port), 15000);
                writeResponse(socket.getOutputStream(), "200 Connection Established");
                log("CONNECT " + target.host + ":" + target.port);
                SocketTunnel.bridge(socket, upstream, workers);
            } catch (IOException err) {
                SocketTunnel.closeQuietly(upstream);
                writeResponse(socket.getOutputStream(), "502 Bad Gateway");
                log("CONNECT " + target.host + ":" + target.port + " failed: " + err.getMessage());
            }
        } catch (IOException ignored) {
        }
    }

    private boolean isAuthorized(String header) {
        if (!auth.isRequired()) {
            return true;
        }
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return false;
            }
            return auth.matches(decoded.substring(0, colon), decoded.substring(colon + 1));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void writeResponse(OutputStream out, String status) throws IOException {
        out.write(("HTTP/1.1 " + status + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static void writeProxyAuthRequired(OutputStream out) throws IOException {
        out.write(("HTTP/1.1 407 Proxy Authentication Required\r\n"
                + "Proxy-Authenticate: Basic realm=\"Loc Relay\"\r\n"
                + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private void log(String line) {
        if (logSink != null) {
            logSink.append(line);
        }
    }

    private static final class HostPort {
        private final String host;
        private final int port;

        private HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        static HostPort parse(String value, int defaultPort) throws IOException {
            int colon = value.lastIndexOf(':');
            if (colon <= 0 || colon == value.length() - 1) {
                return new HostPort(value, defaultPort);
            }
            try {
                return new HostPort(value.substring(0, colon), Integer.parseInt(value.substring(colon + 1)));
            } catch (NumberFormatException err) {
                throw new IOException("invalid port in CONNECT target: " + value, err);
            }
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
