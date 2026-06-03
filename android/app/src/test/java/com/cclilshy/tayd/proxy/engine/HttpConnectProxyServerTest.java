package com.cclilshy.tayd.proxy.engine;

import com.cclilshy.tayd.proxy.domain.ProxyAuth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

public class HttpConnectProxyServerTest {
    @Test
    public void tunnelsBytesAfterConnectHandshake() throws Exception {
        try (EchoServer echo = EchoServer.start();
             HttpConnectProxyServer proxy = new HttpConnectProxyServer(InetAddress.getLoopbackAddress(), 0)) {
            proxy.start();

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.getPort())) {
                OutputStream out = client.getOutputStream();
                out.write(("CONNECT 127.0.0.1:" + echo.getPort() + " HTTP/1.1\r\n"
                        + "Host: 127.0.0.1:" + echo.getPort() + "\r\n"
                        + "\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
                String status = reader.readLine();
                assertTrue(status, status.contains("200"));
                while (!reader.readLine().isEmpty()) {
                    // Drain headers.
                }

                out.write("ping\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                assertEquals("ping", reader.readLine());
            }
        }
    }

    @Test
    public void requiresBasicProxyAuthorizationWhenConfigured() throws Exception {
        try (EchoServer echo = EchoServer.start();
             HttpConnectProxyServer proxy = new HttpConnectProxyServer(
                     InetAddress.getLoopbackAddress(),
                     0,
                     ProxyAuth.from("alice", "secret"))) {
            proxy.start();

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.getPort())) {
                OutputStream out = client.getOutputStream();
                out.write(("CONNECT 127.0.0.1:" + echo.getPort() + " HTTP/1.1\r\n"
                        + "Host: 127.0.0.1:" + echo.getPort() + "\r\n"
                        + "\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        client.getInputStream(),
                        StandardCharsets.US_ASCII));
                assertTrue(reader.readLine().contains("407"));
            }
        }
    }

    @Test
    public void acceptsBasicProxyAuthorizationWhenConfigured() throws Exception {
        try (EchoServer echo = EchoServer.start();
             HttpConnectProxyServer proxy = new HttpConnectProxyServer(
                     InetAddress.getLoopbackAddress(),
                     0,
                     ProxyAuth.from("alice", "secret"))) {
            proxy.start();

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.getPort())) {
                String token = Base64.getEncoder().encodeToString("alice:secret".getBytes(StandardCharsets.UTF_8));
                OutputStream out = client.getOutputStream();
                out.write(("CONNECT 127.0.0.1:" + echo.getPort() + " HTTP/1.1\r\n"
                        + "Host: 127.0.0.1:" + echo.getPort() + "\r\n"
                        + "Proxy-Authorization: Basic " + token + "\r\n"
                        + "\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        client.getInputStream(),
                        StandardCharsets.US_ASCII));
                assertTrue(reader.readLine().contains("200"));
            }
        }
    }

    private static final class EchoServer implements AutoCloseable {
        private final ServerSocket server;
        private final Thread thread;

        private EchoServer(ServerSocket server, Thread thread) {
            this.server = server;
            this.thread = thread;
        }

        static EchoServer start() throws Exception {
            ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread thread = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.getInputStream().transferTo(socket.getOutputStream());
                } catch (Exception ignored) {
                }
            });
            thread.start();
            return new EchoServer(server, thread);
        }

        int getPort() {
            return server.getLocalPort();
        }

        @Override
        public void close() throws Exception {
            server.close();
            thread.join(1000);
        }
    }
}
