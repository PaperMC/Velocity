package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packets.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packets.ServerLogin;
import com.velocitypowered.proxy.protocol.packets.ServerLoginSuccess;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.util.UuidUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection inbound;
    private ServerLogin login;

    public LoginSessionHandler(MinecraftConnection inbound) {
        this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof ServerLogin) {
            this.login = (ServerLogin) packet;
            // TODO: Online-mode checks
            handleSuccessfulLogin();
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

    private void handleSuccessfulLogin() {
        inbound.setCompressionThreshold(256);

        String username = login.getUsername();
        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(username);
        success.setUuid(UuidUtils.generateOfflinePlayerUuid(username));
        inbound.write(success);

        // Initiate a regular connection and move over to it.
        ConnectedPlayer player = new ConnectedPlayer(success.getUsername(), success.getUuid(), inbound);
        ServerInfo info = new ServerInfo("test", new InetSocketAddress("localhost", 25565));
        ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());

        inbound.setState(StateRegistry.PLAY);
        inbound.setSessionHandler(new InitialConnectSessionHandler(player));
        connection.connect();
    }
}
