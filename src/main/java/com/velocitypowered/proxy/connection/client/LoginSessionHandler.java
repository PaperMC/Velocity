package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packets.ServerLogin;
import com.velocitypowered.proxy.protocol.packets.ServerLoginSuccess;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.util.UuidUtils;

import java.net.InetSocketAddress;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection inbound;

    public LoginSessionHandler(MinecraftConnection inbound) {
        this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkArgument(packet instanceof ServerLogin, "Expected a ServerLogin packet, not " + packet.getClass().getName());

        // TODO: Encryption
        inbound.setCompressionThreshold(256);

        String username = ((ServerLogin) packet).getUsername();
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
