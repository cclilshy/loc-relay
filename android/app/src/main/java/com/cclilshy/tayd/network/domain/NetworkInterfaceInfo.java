package com.cclilshy.tayd.network.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NetworkInterfaceInfo {
    private final String name;
    private final String displayName;
    private final boolean up;
    private final boolean loopback;
    private final int mtu;
    private final List<String> addresses;

    public NetworkInterfaceInfo(
            String name,
            String displayName,
            boolean up,
            boolean loopback,
            int mtu,
            List<String> addresses) {
        this.name = name == null ? "" : name;
        this.displayName = displayName;
        this.up = up;
        this.loopback = loopback;
        this.mtu = mtu;
        this.addresses = Collections.unmodifiableList(new ArrayList<>(addresses));
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isLoopback() {
        return loopback;
    }

    public int getMtu() {
        return mtu;
    }

    public List<String> getAddresses() {
        return addresses;
    }
}
