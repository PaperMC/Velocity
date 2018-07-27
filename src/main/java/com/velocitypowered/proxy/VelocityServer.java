package com.velocitypowered.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.network.ConnectionManager;
import com.velocitypowered.proxy.connection.http.NettyHttpClient;
import com.velocitypowered.proxy.util.EncryptionUtils;
import io.netty.bootstrap.Bootstrap;
import net.kyori.text.Component;
import net.kyori.text.serializer.GsonComponentSerializer;

import java.net.InetSocketAddress;
import java.security.KeyPair;

public class VelocityServer {
    private static final VelocityServer INSTANCE = new VelocityServer();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
            .create();

    private final ConnectionManager cm = new ConnectionManager();
    private NettyHttpClient httpClient;
    private KeyPair serverKeyPair;

    private VelocityServer() {
    }

    public static VelocityServer getServer() {
        return INSTANCE;
    }

    public KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

    public void start() {
        // Create a key pair
        serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

        httpClient = new NettyHttpClient(this);

        this.cm.bind(new InetSocketAddress(26671));
    }

    public Bootstrap initializeGenericBootstrap() {
        return this.cm.createWorker();
    }

    public void shutdown() {
        this.cm.shutdown();
    }

    public NettyHttpClient getHttpClient() {
        return httpClient;
    }
}
