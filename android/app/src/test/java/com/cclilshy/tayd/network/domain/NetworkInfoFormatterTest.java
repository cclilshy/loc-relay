package com.cclilshy.tayd.network.domain;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class NetworkInfoFormatterTest {
    @Test
    public void rendersInterfacesAsStructuredJsonLikeText() {
        NetworkInterfaceInfo wlan = new NetworkInterfaceInfo(
                "wlan0",
                "Wi-Fi",
                true,
                false,
                1500,
                Arrays.asList("192.168.1.10/24", "fe80::1/64"));
        NetworkInterfaceInfo loopback = new NetworkInterfaceInfo(
                "lo",
                null,
                true,
                true,
                65536,
                Collections.singletonList("127.0.0.1/8"));

        String output = NetworkInfoFormatter.format(Arrays.asList(wlan, loopback));

        assertTrue(output.contains("\"interfaces\""));
        assertTrue(output.contains("\"name\": \"wlan0\""));
        assertTrue(output.contains("\"displayName\": \"Wi-Fi\""));
        assertTrue(output.contains("\"up\": true"));
        assertTrue(output.contains("\"loopback\": false"));
        assertTrue(output.contains("\"mtu\": 1500"));
        assertTrue(output.contains("\"192.168.1.10/24\""));
        assertTrue(output.contains("\"addresses\": [\n        \"192.168.1.10/24\",\n        \"fe80::1/64\"\n      ]"));
        assertTrue(output.contains("\"name\": \"lo\""));
    }
}
