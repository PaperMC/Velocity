package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.util.EncryptionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;

public class LoginSessionHandler implements MinecraftSessionHandler {

    private static final Logger logger = LogManager.getLogger(LoginSessionHandler.class);
    private static final String MOJANG_SERVER_AUTH_URL =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s&ip=%s";

    private final VelocityServer server;
    private final MinecraftConnection inbound;
    private final InboundConnection apiInbound;
    private @MonotonicNonNull ServerLogin login;
    private byte[] verify = EMPTY_BYTE_ARRAY;
    private int playerInfoId;
    private @MonotonicNonNull ConnectedPlayer connectedPlayer;

    public LoginSessionHandler(VelocityServer server, MinecraftConnection inbound, InboundConnection apiInbound) {
        this.server = Preconditions.checkNotNull(server, "server");
        this.inbound = Preconditions.checkNotNull(inbound, "inbound");
        this.apiInbound = Preconditions.checkNotNull(apiInbound, "apiInbound");
    }

    @Override
    public boolean handle(ServerLogin packet) {
        this.login = packet;
        if (inbound.getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_13) {
            playerInfoId = ThreadLocalRandom.current().nextInt();
            inbound.write(new LoginPluginMessage(playerInfoId, VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL,
                    Unpooled.EMPTY_BUFFER));
        } else {
            beginPreLogin();
        }
        return true;
    }

    @Override
    public boolean handle(LoginPluginResponse packet) {
        if (packet.getId() == playerInfoId) {
            if (packet.isSuccess()) {
                // Uh oh, someone's trying to run Velocity behind Velocity. We don't want that happening.
                inbound.closeWith(Disconnect.create(
                        TextComponent.of("Running Velocity behind Velocity isn't supported.", TextColor.RED)
                ));
            } else {
                // Proceed with the regular login process.
                beginPreLogin();
            }
        }
        return true;
    }

    @Override
    public boolean handle(EncryptionResponse packet) {
        ServerLogin login = this.login;
        if (login == null) {
            throw new IllegalStateException("No ServerLogin packet received yet.");
        }

        if (verify.length == 0) {
            throw new IllegalStateException("No EncryptionRequest packet sent yet.");
        }

        try {
            KeyPair serverKeyPair = server.getServerKeyPair();
            byte[] decryptedVerifyToken = EncryptionUtils.decryptRsa(serverKeyPair, packet.getVerifyToken());
            if (!Arrays.equals(verify, decryptedVerifyToken)) {
                throw new IllegalStateException("Unable to successfully decrypt the verification token.");
            }

            byte[] decryptedSharedSecret = EncryptionUtils.decryptRsa(serverKeyPair, packet.getSharedSecret());
            String serverId = EncryptionUtils.generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

            String playerIp = ((InetSocketAddress) inbound.getRemoteAddress()).getHostString();
            server.getHttpClient()
                    .get(new URL(String.format(MOJANG_SERVER_AUTH_URL, login.getUsername(), serverId, playerIp)))
                    .thenAcceptAsync(profileResponse -> {
                        if (inbound.isClosed()) {
                            // The player disconnected after we authenticated them.
                            return;
                        }

                        // Go ahead and enable encryption. Once the client sends EncryptionResponse, encryption is
                        // enabled.
                        try {
                            inbound.enableEncryption(decryptedSharedSecret);
                        } catch (GeneralSecurityException e) {
                            throw new RuntimeException(e);
                        }

                        if (profileResponse.getCode() == 200) {
                            // All went well, initialize the session.
                            initializePlayer(VelocityServer.GSON.fromJson(profileResponse.getBody(), GameProfile.class), true);
                        } else if (profileResponse.getCode() == 204) {
                            // Apparently an offline-mode user logged onto this online-mode proxy. The client has enabled
                            // encryption, so we need to do that as well.
                            logger.warn("An offline-mode client ({} from {}) tried to connect!", login.getUsername(), playerIp);
                            inbound.closeWith(Disconnect.create(TextComponent.of("This server only accepts connections from online-mode clients.")));
                        } else {
                            // Something else went wrong
                            logger.error("Got an unexpected error code {} whilst contacting Mojang to log in {} ({})",
                                    profileResponse.getCode(), login.getUsername(), playerIp);
                            inbound.close();
                        }
                    }, inbound.eventLoop())
                    .exceptionally(exception -> {
                        logger.error("Unable to enable encryption", exception);
                        inbound.close();
                        return null;
                    });
        } catch (GeneralSecurityException e) {
            logger.error("Unable to enable encryption", e);
            inbound.close();
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
        return true;
    }

    private void beginPreLogin() {
        ServerLogin login = this.login;
        if (login == null) {
            throw new IllegalStateException("No ServerLogin packet received yet.");
        }
        PreLoginEvent event = new PreLoginEvent(apiInbound, login.getUsername());
        server.getEventManager().fire(event)
                .thenRunAsync(() -> {
                    if (inbound.isClosed()) {
                        // The player was disconnected
                        return;
                    }
                    PreLoginComponentResult result = event.getResult();
                    Optional<Component> disconnectReason = result.getReason();
                    if (disconnectReason.isPresent()) {
                        // The component is guaranteed to be provided if the connection was denied.
                        inbound.closeWith(Disconnect.create(disconnectReason.get()));
                        return;
                    }

                    if (!result.isForceOfflineMode() && (server.getConfiguration().isOnlineMode() || result.isOnlineModeAllowed())) {
                        // Request encryption.
                        EncryptionRequest request = generateRequest();
                        this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
                        inbound.write(request);
                    } else {
                        initializePlayer(GameProfile.forOfflinePlayer(login.getUsername()), false);
                    }
                }, inbound.eventLoop());
    }

    private EncryptionRequest generateRequest() {
        byte[] verify = new byte[4];
        ThreadLocalRandom.current().nextBytes(verify);

        EncryptionRequest request = new EncryptionRequest();
        request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
        request.setVerifyToken(verify);
        return request;
    }

    private void initializePlayer(GameProfile profile, boolean onlineMode) {
        if (inbound.isLegacyForge() && server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.LEGACY) {
            // We want to add the FML token to the properties
            List<GameProfile.Property> properties = new ArrayList<>(profile.getProperties());
            properties.add(new GameProfile.Property("forgeClient", "true", ""));
            profile = new GameProfile(profile.getId(), profile.getName(), properties);
        }
        GameProfileRequestEvent profileRequestEvent = new GameProfileRequestEvent(apiInbound, profile, onlineMode);

        server.getEventManager().fire(profileRequestEvent).thenCompose(profileEvent -> {
            // Initiate a regular connection and move over to it.
            ConnectedPlayer player = new ConnectedPlayer(server, profileEvent.getGameProfile(), inbound,
                    apiInbound.getVirtualHost().orElse(null));
            this.connectedPlayer = player;

            return server.getEventManager().fire(new PermissionsSetupEvent(player, ConnectedPlayer.DEFAULT_PERMISSIONS))
                        .thenCompose(event -> {
                            // wait for permissions to load, then set the players permission function
                            player.setPermissionFunction(event.createFunction(player));
                            // then call & wait for the login event
                            return server.getEventManager().fire(new LoginEvent(player));
                        })
                        // then complete the connection
                        .thenAcceptAsync(event -> {
                            if (inbound.isClosed()) {
                                // The player was disconnected
                                return;
                            }

                            Optional<Component> reason = event.getResult().getReason();
                            if (reason.isPresent()) {
                                player.disconnect(reason.get());
                            } else {
                                handleProxyLogin(player);
                            }
                        }, inbound.eventLoop());
        });

    }

    private void handleProxyLogin(ConnectedPlayer player) {
        Optional<RegisteredServer> toTry = player.getNextServerToTry();
        if (!toTry.isPresent()) {
            player.close(TextComponent.of("No available servers", TextColor.RED));
            return;
        }

        if (!server.registerConnection(player)) {
            inbound.closeWith(Disconnect.create(TextComponent.of("You are already on this proxy!", TextColor.RED)));
            return;
        }

        int threshold = server.getConfiguration().getCompressionThreshold();
        if (threshold >= 0) {
            inbound.write(new SetCompression(threshold));
            inbound.setCompressionThreshold(threshold);
        }

        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(player.getUsername());
        success.setUuid(player.getUniqueId());
        inbound.write(success);

        inbound.setAssociation(player);
        inbound.setState(StateRegistry.PLAY);

        logger.info("{} has connected", player);
        inbound.setSessionHandler(new InitialConnectSessionHandler(player));
        server.getEventManager().fire(new PostLoginEvent(player)).thenRun(() -> player.createConnectionRequest(toTry.get()).fireAndForget());
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        throw new IllegalStateException("Unknown data " + ByteBufUtil.hexDump(buf));
    }

    @Override
    public void disconnected() {
        if (connectedPlayer != null) {
            connectedPlayer.teardown();
        }
    }
}
