package com.cclilshy.tayd.proxy.engine;

import com.cclilshy.tayd.proxy.domain.ProxyAuth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class Socks5ProxyServerTest {
    @Test
    public void tunnelsBytesAfterConnectHandshake() throws Exception {
        try (EchoServer echo = EchoServer.start();
             Socks5ProxyServer proxy = new Socks5ProxyServer(InetAddress.getLoopbackAddress(), 0)) {
            proxy.start();

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.getPort())) {
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();

                out.write(new byte[] {0x05, 0x01, 0x00});
                out.flush();
                assertArrayEquals(new byte[] {0x05, 0x00}, readExactly(in, 2));

                int port = echo.getPort();
                out.write(new byte[] {
                        0x05, 0x01, 0x00, 0x01,
                        127, 0, 0, 1,
                        (byte) (port >> 8), (byte) port
                });
                out.flush();
                byte[] response = readExactly(in, 10);
                assertEquals(0x05, response[0] & 0xff);
                assertEquals(0x00, response[1] & 0xff);

                out.write("ping\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                assertArrayEquals("ping\n".getBytes(StandardCharsets.US_ASCII), readExactly(in, 5));
            }
        }
    }

    @Test
    public void requiresUsernamePasswordWhenConfigured() throws Exception {
        try (Socks5ProxyServer proxy = new Socks5ProxyServer(
                InetAddress.getLoopbackAddress(),
                0,
                ProxyAuth.from("alice", "secret"))) {
            proxy.start();

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.getPort())) {
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();

                out.write(new byte[] {0x05, 0x01, 0x00});
                out.flush();

                assertArrayEquals(new byte[] {0x05, (byte) 0xff}, readExactly(in, 2));
            }
        }
    }

    @Test
    public void acceptsUsernamePasswordWhenConfigured() throws Exception {
        try (EchoServer echo = EchoServer.start();
             Socks5ProxyServer proxy = new Socks5ProxyServer(
                     InetAddress.getLoopbackAddress(),
                     0,
                     ProxyAuth.from("alice", "secret"))) {
            proxy.start();

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.getPort())) {
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();

                out.write(new byte[] {0x05, 0x01, 0x02});
                out.flush();
                assertArrayEquals(new byte[] {0x05, 0x02}, readExactly(in, 2));

                out.write(new byte[] {0x01, 0x05});
                out.write("alice".getBytes(StandardCharsets.US_ASCII));
                out.write(new byte[] {0x06});
                out.write("secret".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                assertArrayEquals(new byte[] {0x01, 0x00}, readExactly(in, 2));

                int port = echo.getPort();
                out.write(new byte[] {
                        0x05, 0x01, 0x00, 0x01,
                        127, 0, 0, 1,
                        (byte) (port >> 8), (byte) port
                });
                out.flush();
                assertEquals(0x00, readExactly(in, 10)[1] & 0xff);
            }
        }
    }

    private static byte[] readExactly(InputStream in, int length) throws Exception {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(data, offset, length - offset);
            if (read == -1) {
                throw new AssertionError("unexpected EOF");
            }
            offset += read;
        }
        return data;
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
