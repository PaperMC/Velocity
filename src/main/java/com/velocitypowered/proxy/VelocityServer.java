package com.velocitypowered.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.network.ConnectionManager;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.http.NettyHttpClient;
import com.velocitypowered.proxy.util.EncryptionUtils;
import io.netty.bootstrap.Bootstrap;
import net.kyori.text.Component;
import net.kyori.text.serializer.GsonComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.security.KeyPair;

public class VelocityServer {
    private static final Logger logger = LogManager.getLogger(VelocityServer.class);
    private static final VelocityServer INSTANCE = new VelocityServer();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
            .create();

    private final ConnectionManager cm = new ConnectionManager();
    private VelocityConfiguration configuration;
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

    public VelocityConfiguration getConfiguration() {
        return configuration;
    }

    public void start() {
        // Create a key pair
        logger.info("Booting up Velocity...");
        try {
            configuration = VelocityConfiguration.read(Paths.get("velocity.toml"));
            if (!configuration.validate()) {
                logger.error("Your configuration is invalid. Velocity will refuse to start up until the errors are resolved.");
                System.exit(1);
            }
        } catch (IOException e) {
            logger.error("Unable to load your velocity.toml. The server will shut down.", e);
            System.exit(1);
        }
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
