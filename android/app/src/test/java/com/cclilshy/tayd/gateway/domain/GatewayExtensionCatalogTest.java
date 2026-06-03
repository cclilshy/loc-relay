package com.cclilshy.tayd.gateway.domain;

import com.cclilshy.tayd.gateway.data.GatewayPrefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.junit.Test;

public class GatewayExtensionCatalogTest {
    @Test
    public void exposesBuiltInGatewayExtensionsInStableOrder() {
        List<GatewayExtension> extensions = GatewayExtensionCatalog.all();

        assertEquals(5, extensions.size());
        assertEquals("frpc", extensions.get(0).getId());
        assertEquals("FRPC", extensions.get(0).getTitle());
        assertEquals("Built-in", extensions.get(0).getBadgeLabel());
        assertEquals("webhook", extensions.get(1).getId());
        assertEquals("WebHook", extensions.get(1).getTitle());
        assertEquals("Built-in", extensions.get(1).getBadgeLabel());
        assertEquals("http", extensions.get(2).getId());
        assertEquals("HTTP PROXY", extensions.get(2).getTitle());
        assertEquals("", extensions.get(2).getBadgeLabel());
        assertEquals("socks5", extensions.get(3).getId());
        assertEquals("SOCKS5", extensions.get(3).getTitle());
        assertEquals("event_listener", extensions.get(4).getId());
        assertEquals("Event Listener", extensions.get(4).getTitle());
    }

    @Test
    public void findsExtensionsById() {
        GatewayExtension extension = GatewayExtensionCatalog.findById("socks5");

        assertEquals("socks5", extension.getId());
        assertEquals("Local port", extension.getFields().get(0).getLabel());
        assertEquals(GatewayPrefs.KEY_SOCKS_PORT, extension.getFields().get(0).getPrefKey());
    }

    @Test
    public void builtInProxyRemotePortsAreNotCoreServiceFields() {
        assertFalse(hasField(GatewayExtensionCatalog.findById("http"), GatewayPrefs.KEY_HTTP_REMOTE_PORT));
        assertFalse(hasField(GatewayExtensionCatalog.findById("socks5"), GatewayPrefs.KEY_SOCKS_REMOTE_PORT));
        assertFalse(hasField(GatewayExtensionCatalog.findById("frpc"), GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS));
        assertEquals(0, GatewayExtensionCatalog.findById("webhook").getFields().size());
        assertEquals(0, GatewayExtensionCatalog.findById("event_listener").getFields().size());
    }

    private static boolean hasField(GatewayExtension extension, String prefKey) {
        for (GatewayExtension.Field field : extension.getFields()) {
            if (field.getPrefKey().equals(prefKey)) {
                return true;
            }
        }
        return false;
    }
}
