package com.cclilshy.tayd.proxy.engine;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class SocketTunnel {
    private SocketTunnel() {
    }

    public static void bridge(Socket left, Socket right, ExecutorService executor) {
        Future<?> leftToRight = executor.submit(() -> {
            try {
                StreamPump.copy(left.getInputStream(), right.getOutputStream());
            } catch (IOException ignored) {
            } finally {
                closeQuietly(right);
            }
        });
        Future<?> rightToLeft = executor.submit(() -> {
            try {
                StreamPump.copy(right.getInputStream(), left.getOutputStream());
            } catch (IOException ignored) {
            } finally {
                closeQuietly(left);
            }
        });
        waitQuietly(leftToRight);
        waitQuietly(rightToLeft);
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private static void waitQuietly(Future<?> future) {
        try {
            future.get();
        } catch (Exception ignored) {
        }
    }
}
