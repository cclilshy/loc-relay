package com.cclilshy.tayd.network.android;

import com.cclilshy.tayd.network.domain.NetworkInterfaceInfo;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

public final class NetworkInfoProvider {
    private NetworkInfoProvider() {
    }

    public static List<NetworkInterfaceInfo> collect() {
        List<NetworkInterfaceInfo> items = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return items;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface item = interfaces.nextElement();
                items.add(toInfo(item));
            }
        } catch (SocketException ignored) {
        }
        Collections.sort(items, Comparator.comparing(NetworkInterfaceInfo::getName));
        return items;
    }

    private static NetworkInterfaceInfo toInfo(NetworkInterface item) {
        return new NetworkInterfaceInfo(
                item.getName(),
                item.getDisplayName(),
                readUp(item),
                readLoopback(item),
                readMtu(item),
                readAddresses(item));
    }

    private static boolean readUp(NetworkInterface item) {
        try {
            return item.isUp();
        } catch (SocketException ignored) {
            return false;
        }
    }

    private static boolean readLoopback(NetworkInterface item) {
        try {
            return item.isLoopback();
        } catch (SocketException ignored) {
            return false;
        }
    }

    private static int readMtu(NetworkInterface item) {
        try {
            return item.getMTU();
        } catch (SocketException ignored) {
            return -1;
        }
    }

    private static List<String> readAddresses(NetworkInterface item) {
        List<String> addresses = new ArrayList<>();
        for (InterfaceAddress address : item.getInterfaceAddresses()) {
            if (address.getAddress() != null) {
                addresses.add(address.getAddress().getHostAddress() + "/" + address.getNetworkPrefixLength());
            }
        }
        Collections.sort(addresses);
        return addresses;
    }
}
