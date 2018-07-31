package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;

import java.net.InetSocketAddress;
import java.net.URI;

public enum AddressUtil {
    ;

    public static InetSocketAddress parseAddress(String ip) {
        Preconditions.checkNotNull(ip, "ip");
        URI uri = URI.create("tcp://" + ip);
        return new InetSocketAddress(uri.getHost(), uri.getPort());
    }
}
