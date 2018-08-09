package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.data.GameProfile;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.api.server.ServerInfo;
import com.velocitypowered.proxy.util.EncryptionUtils;
import io.netty.buffer.Unpooled;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private static final Logger logger = LogManager.getLogger(LoginSessionHandler.class);
    private static final String MOJANG_SERVER_AUTH_URL =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s&ip=%s";

    private final MinecraftConnection inbound;
    private ServerLogin login;
    private byte[] verify;
    private int playerInfoId;

    public LoginSessionHandler(MinecraftConnection inbound) {
        this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    }

    @Override
    public void activated() {
        if (inbound.getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_13) {
            LoginPluginMessage message = new LoginPluginMessage();
            playerInfoId = ThreadLocalRandom.current().nextInt();
            message.setId(playerInfoId);
            message.setChannel(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL);
            message.setData(Unpooled.EMPTY_BUFFER);
            inbound.write(message);
        }
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof LoginPluginResponse) {
            LoginPluginResponse lpr = (LoginPluginResponse) packet;
            if (lpr.getId() == playerInfoId && lpr.isSuccess()) {
                // Uh oh, someone's trying to run Velocity behind Velocity. We don't want that happening.
                inbound.closeWith(Disconnect.create(
                        TextComponent.of("Running Velocity behind Velocity isn't supported.", TextColor.RED)
                ));
            }
        } else if (packet instanceof ServerLogin) {
            this.login = (ServerLogin) packet;

            if (VelocityServer.getServer().getConfiguration().isOnlineMode()) {
                // Request encryption.
                EncryptionRequest request = generateRequest();
                this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
                inbound.write(request);
            } else {
                // Offline-mode, don't try to request encryption.
                handleSuccessfulLogin(GameProfile.forOfflinePlayer(login.getUsername()));
            }
        } else if (packet instanceof EncryptionResponse) {
            try {
                KeyPair serverKeyPair = VelocityServer.getServer().getServerKeyPair();
                EncryptionResponse response = (EncryptionResponse) packet;
                byte[] decryptedVerifyToken = EncryptionUtils.decryptRsa(serverKeyPair, response.getVerifyToken());
                if (!Arrays.equals(verify, decryptedVerifyToken)) {
                    throw new IllegalStateException("Unable to successfully decrypt the verification token.");
                }

                byte[] decryptedSharedSecret = EncryptionUtils.decryptRsa(serverKeyPair, response.getSharedSecret());
                String serverId = EncryptionUtils.generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

                String playerIp = ((InetSocketAddress) inbound.getChannel().remoteAddress()).getHostString();
                VelocityServer.getServer().getHttpClient()
                        .get(new URL(String.format(MOJANG_SERVER_AUTH_URL, login.getUsername(), serverId, playerIp)))
                        .thenAcceptAsync(profileResponse -> {
                            if (inbound.isClosed()) {
                                // The player disconnected after we authenticated them.
                                return;
                            }

                            try {
                                inbound.enableEncryption(decryptedSharedSecret);
                            } catch (GeneralSecurityException e) {
                                throw new RuntimeException(e);
                            }

                            GameProfile profile = VelocityServer.GSON.fromJson(profileResponse, GameProfile.class);
                            handleSuccessfulLogin(profile);
                        }, inbound.getChannel().eventLoop())
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
        }
    }

    private EncryptionRequest generateRequest() {
        byte[] verify = new byte[4];
        ThreadLocalRandom.current().nextBytes(verify);

        EncryptionRequest request = new EncryptionRequest();
        request.setPublicKey(VelocityServer.getServer().getServerKeyPair().getPublic().getEncoded());
        request.setVerifyToken(verify);
        return request;
    }

    private void handleSuccessfulLogin(GameProfile profile) {
        // Initiate a regular connection and move over to it.
        ConnectedPlayer player = new ConnectedPlayer(profile, inbound);
        Optional<ServerInfo> toTry = player.getNextServerToTry();
        if (!toTry.isPresent()) {
            player.close(TextComponent.of("No available servers", TextColor.RED));
            return;
        }

        int threshold = VelocityServer.getServer().getConfiguration().getCompressionThreshold();
        if (threshold >= 0) {
            inbound.write(new SetCompression(threshold));
            inbound.setCompressionThreshold(threshold);
        }

        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(profile.getName());
        success.setUuid(profile.idAsUuid());
        inbound.write(success);

        inbound.setAssociation(player);
        inbound.setState(StateRegistry.PLAY);

        if (!VelocityServer.getServer().registerConnection(player)) {
            inbound.closeWith(Disconnect.create(TextComponent.of("You are already on this proxy!", TextColor.RED)));
        }

        logger.info("{} has connected", player);
        inbound.setSessionHandler(new InitialConnectSessionHandler(player));
        player.createConnectionRequest(toTry.get()).fireAndForget();
    }
}
