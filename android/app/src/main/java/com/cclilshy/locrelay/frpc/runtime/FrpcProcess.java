package com.cclilshy.locrelay.frpc.runtime;

import com.cclilshy.locrelay.proxy.domain.ProxyMapping;
import com.cclilshy.locrelay.frpc.domain.FrpcConfigRenderer;
import com.cclilshy.locrelay.frpc.data.FrpcProxyStore;
import com.cclilshy.locrelay.gateway.data.GatewaySettings;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

public final class FrpcProcess implements AutoCloseable {
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\u001b\\[[0-?]*[ -/]*[@-~]");
    private Process process;
    private Thread logThread;

    public boolean start(Context context, GatewaySettings settings, StatusSink statusSink) throws IOException {
        File binary = new File(context.getApplicationInfo().nativeLibraryDir, "libfrpc.so");
        if (!binary.canExecute()) {
            statusSink.append("frpc disabled: libfrpc.so not packaged for this ABI");
            return false;
        }
        File config = writeConfig(context, settings);
        Process started = new ProcessBuilder(binary.getAbsolutePath(), "-c", config.getAbsolutePath())
                .directory(context.getFilesDir())
                .redirectErrorStream(true)
                .start();
        process = started;
        logThread = new Thread(() -> streamLogs(started, statusSink), "frpc-log");
        logThread.setDaemon(true);
        logThread.start();
        statusSink.append("frpc started");
        return true;
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (logThread != null) {
            logThread.interrupt();
            logThread = null;
        }
    }

    private static File writeConfig(Context context, GatewaySettings settings) throws IOException {
        File frpDir = new File(context.getFilesDir(), "frp");
        File includeDir = new File(frpDir, "frpc.d");
        if (!includeDir.exists() && !includeDir.mkdirs()) {
            throw new IOException("could not create " + includeDir);
        }
        List<ProxyMapping> mappings = FrpcProxyStore.parse(settings.frpcProxyMappings);
        String config = FrpcConfigRenderer.renderClient(
                settings.frpcServer,
                settings.frpcServerPort,
                settings.frpcToken,
                includeDir.getAbsolutePath(),
                mappings);
        File configFile = new File(frpDir, "frpc.toml");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile),
                StandardCharsets.UTF_8)) {
            writer.write(config);
        }
        return configFile;
    }

    public static String frpcLocalIp(String bindHost) {
        String host = bindHost == null ? "" : bindHost.trim();
        if (host.isEmpty() || "0.0.0.0".equals(host) || "::".equals(host) || "[::]".equals(host)) {
            return "127.0.0.1";
        }
        return host;
    }

    public static void streamLogs(Process process, StatusSink statusSink) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                statusSink.append("frpc: " + stripAnsi(line));
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                statusSink.append("frpc exited with code " + exitCode);
            }
        } catch (IOException ignored) {
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static String stripAnsi(String value) {
        return ANSI_ESCAPE.matcher(value).replaceAll("");
    }

    public interface StatusSink {
        void append(String line);
    }
}
