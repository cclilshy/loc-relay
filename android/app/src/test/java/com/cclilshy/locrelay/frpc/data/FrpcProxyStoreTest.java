package com.cclilshy.locrelay.frpc.data;

import com.cclilshy.locrelay.proxy.domain.ProxyMapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class FrpcProxyStoreTest {
    @Test
    public void parsesExplicitProxyMappings() {
        List<ProxyMapping> mappings = FrpcProxyStore.parse("http|tcp|127.0.0.1|8080|28080\nssh|tcp|127.0.0.1|22|60022");

        assertEquals(2, mappings.size());
        assertEquals("http", mappings.get(0).getName());
        assertEquals(8080, mappings.get(0).getLocalPort());
        assertEquals(60022, mappings.get(1).getRemotePort());
    }

    @Test
    public void formatsExplicitProxyMappings() {
        String output = FrpcProxyStore.format(Arrays.asList(
                new ProxyMapping("http", "tcp", "127.0.0.1", 8080, 28080),
                new ProxyMapping("socks5", "tcp", "127.0.0.1", 1080, 21080)));

        assertEquals("http|tcp|127.0.0.1|8080|28080\nsocks5|tcp|127.0.0.1|1080|21080", output);
    }

    @Test
    public void hasMappingReturnsFalseForMissingOrInvalidMappings() {
        assertTrue(FrpcProxyStore.hasMapping("http|tcp|127.0.0.1|8080|28080", "http"));
        assertFalse(FrpcProxyStore.hasMapping("http|tcp|127.0.0.1|8080|28080", "socks5"));
        assertFalse(FrpcProxyStore.hasMapping("not-a-valid-mapping", "http"));
    }

    @Test
    public void removesMappingByNameWithoutTouchingCustomMappings() {
        String input = "http|tcp|127.0.0.1|8080|28080\nssh|tcp|127.0.0.1|22|60022";

        String output = FrpcProxyStore.remove(input, "http");

        assertEquals("ssh|tcp|127.0.0.1|22|60022", output);
    }

    @Test
    public void findsMappingByName() {
        ProxyMapping mapping = FrpcProxyStore.find(
                "http|tcp|127.0.0.1|8080|28080\nsocks5|tcp|127.0.0.1|1080|21080",
                "socks5");

        assertEquals(21080, mapping.getRemotePort());
    }
}
