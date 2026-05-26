package com.cclilshy.locrelay.frpc.runtime;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class FrpcProcessTest {
    @Test
    public void streamLogsStripsAnsiCodesAndReportsNonZeroExit() {
        FakeProcess process = new FakeProcess("\u001b[1;34mlogin failed\u001b[0m\n", 2);
        List<String> lines = new ArrayList<>();

        FrpcProcess.streamLogs(process, lines::add);

        assertEquals(Arrays.asList(
                "frpc: login failed",
                "frpc exited with code 2"), lines);
    }

    @Test
    public void frpcLocalIpUsesConcreteBindHostWhenProxyIsNotOnLoopback() {
        assertEquals("127.0.0.1", FrpcProcess.frpcLocalIp("0.0.0.0"));
        assertEquals("127.0.0.1", FrpcProcess.frpcLocalIp("::"));
        assertEquals("192.168.1.50", FrpcProcess.frpcLocalIp("192.168.1.50"));
    }

    private static final class FakeProcess extends Process {
        private final InputStream inputStream;
        private final int exitCode;

        private FakeProcess(String output, int exitCode) {
            this.inputStream = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
            this.exitCode = exitCode;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            return exitCode;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
        }
    }
}
