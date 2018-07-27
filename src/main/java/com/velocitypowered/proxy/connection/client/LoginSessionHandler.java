package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.data.GameProfile;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packets.*;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.util.EncryptionUtils;
import com.velocitypowered.proxy.util.UuidUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private static final Logger logger = LogManager.getLogger(LoginSessionHandler.class);

    private static final String MOJANG_SERVER_AUTH_URL =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s&ip=%s";

    private final MinecraftConnection inbound;
    private ServerLogin login;
    private byte[] verify;

    public LoginSessionHandler(MinecraftConnection inbound) {
        this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    }

    @Override
    public void handle(MinecraftPacket packet) throws Exception {
        if (packet instanceof ServerLogin) {
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
        }

        if (packet instanceof EncryptionResponse) {
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
        inbound.write(new SetCompression(256));
        inbound.setCompressionThreshold(256);

        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(profile.getName());
        success.setUuid(profile.idAsUuid());
        inbound.write(success);

        // Initiate a regular connection and move over to it.
        ConnectedPlayer player = new ConnectedPlayer(profile, inbound);
        logger.info("{} has connected", player);
        inbound.setAssociation(player);
        ServerInfo info = new ServerInfo("test", new InetSocketAddress("localhost", 25565));
        ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());

        inbound.setState(StateRegistry.PLAY);
        inbound.setSessionHandler(new InitialConnectSessionHandler(player));
        connection.connect();
    }
}
