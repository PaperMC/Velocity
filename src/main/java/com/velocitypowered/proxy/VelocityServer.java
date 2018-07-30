package com.velocitypowered.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.network.ConnectionManager;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.http.NettyHttpClient;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.proxy.util.EncryptionUtils;
import com.velocitypowered.proxy.util.ServerMap;
import io.netty.bootstrap.Bootstrap;
import net.kyori.text.Component;
import net.kyori.text.serializer.GsonComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

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
    private final ServerMap servers = new ServerMap();

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
            Path configPath = Paths.get("velocity.toml");
            try {
                configuration = VelocityConfiguration.read(configPath);
            } catch (NoSuchFileException e) {
                logger.info("No velocity.toml found, creating one for you...");
                Files.copy(VelocityServer.class.getResourceAsStream("/velocity.toml"), configPath);
                configuration = VelocityConfiguration.read(configPath);
            }

            if (!configuration.validate()) {
                logger.error("Your configuration is invalid. Velocity will refuse to start up until the errors are resolved.");
                System.exit(1);
            }
        } catch (IOException e) {
            logger.error("Unable to load your velocity.toml. The server will shut down.", e);
            System.exit(1);
        }

        for (Map.Entry<String, String> entry : configuration.getServers().entrySet()) {
            servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue())));
        }

        serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

        httpClient = new NettyHttpClient(this);

        this.cm.bind(configuration.getBind());
    }

    public ServerMap getServers() {
        return servers;
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
