package com.cclilshy.tayd.proxy.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

public final class StreamPump {
    private static final int BUFFER_SIZE = 16 * 1024;

    private StreamPump() {
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            int read;
            try {
                read = in.read(buffer);
            } catch (SocketException ignored) {
                return;
            }
            if (read == -1) {
                return;
            }
            out.write(buffer, 0, read);
            out.flush();
        }
    }
}
