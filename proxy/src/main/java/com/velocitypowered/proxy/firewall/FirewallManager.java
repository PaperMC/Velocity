package com.velocitypowered.proxy.firewall;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class FirewallManager {

    private static final Cache<String, Byte> blockedTemporary = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    public static final HashSet<String> whitelistedAddresses = new HashSet<>();

    public static void remove(String hostname) {
        blockedTemporary.invalidate(hostname);
    }

    public static void add(String hostname) {
        if (!whitelistedAddresses.contains(hostname)) {
            blockedTemporary.put(hostname, (byte) 0);
        }
    }

    public static boolean isFirewalled(String address) {
        return blockedTemporary.getIfPresent(address) != null;
    }

}