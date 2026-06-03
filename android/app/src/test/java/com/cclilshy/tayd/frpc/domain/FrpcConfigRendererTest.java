package com.cclilshy.tayd.frpc.domain;

import com.cclilshy.tayd.proxy.domain.ProxyMapping;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class FrpcConfigRendererTest {
    @Test
    public void rendersServerAuthAndAppPrivateIncludes() {
        ProxyMapping mapping = new ProxyMapping("phone-api", "tcp", "127.0.0.1", 8080, 18080);

        String config = FrpcConfigRenderer.renderClient(
                "frp.example.com",
                7000,
                "token\"with\\slashes",
                "/data/user/0/com.cclilshy.tayd/files/frpc.d",
                Collections.singletonList(mapping));

        assertTrue(config.contains("serverAddr = \"frp.example.com\""));
        assertTrue(config.contains("serverPort = 7000"));
        assertTrue(config.contains("auth.token = \"token\\\"with\\\\slashes\""));
        assertTrue(config.contains("includes = [\"/data/user/0/com.cclilshy.tayd/files/frpc.d/*.toml\"]"));
    }

    @Test
    public void rendersProxyBlocks() {
        ProxyMapping mapping = new ProxyMapping("phone-api", "tcp", "127.0.0.1", 8080, 18080);

        String proxy = FrpcConfigRenderer.renderProxy(mapping);

        assertTrue(proxy.contains("[[proxies]]"));
        assertTrue(proxy.contains("name = \"phone-api\""));
        assertTrue(proxy.contains("type = \"tcp\""));
        assertTrue(proxy.contains("localIP = \"127.0.0.1\""));
        assertTrue(proxy.contains("localPort = 8080"));
        assertTrue(proxy.contains("remotePort = 18080"));
    }
}
