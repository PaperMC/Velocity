package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.text.TextComponent;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final VelocityServer server;
    private final VelocityServerConnection serverConn;
    private final CompletableFuture<ConnectionRequestBuilder.Result> resultFuture;
    private boolean informationForwarded;

    LoginSessionHandler(VelocityServer server, VelocityServerConnection serverConn,
                        CompletableFuture<ConnectionRequestBuilder.Result> resultFuture) {
        this.server = server;
        this.serverConn = serverConn;
        this.resultFuture = resultFuture;
    }

    @Override
    public boolean handle(EncryptionRequest packet) {
        throw new IllegalStateException("Backend server is online-mode!");
    }

    @Override
    public boolean handle(LoginPluginMessage packet) {
        VelocityConfiguration configuration = server.getConfiguration();
        if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN && packet.getChannel()
                .equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {
            LoginPluginResponse response = new LoginPluginResponse();
            response.setSuccess(true);
            response.setId(packet.getId());
            response.setData(createForwardingData(configuration.getForwardingSecret(),
                    serverConn.getPlayer().getRemoteAddress().getHostString(),
                    serverConn.getPlayer().getProfile()));
            serverConn.getConnection().write(response);
            informationForwarded = true;
        } else {
            // Don't understand
            LoginPluginResponse response = new LoginPluginResponse();
            response.setSuccess(false);
            response.setId(packet.getId());
            response.setData(Unpooled.EMPTY_BUFFER);
            serverConn.getConnection().write(response);
        }
        return true;
    }

    @Override
    public boolean handle(Disconnect packet) {
        resultFuture.complete(ConnectionRequestResults.forDisconnect(packet));
        serverConn.disconnect();
        return true;
    }

    @Override
    public boolean handle(SetCompression packet) {
        serverConn.getConnection().setCompressionThreshold(packet.getThreshold());
        return true;
    }

    @Override
    public boolean handle(ServerLoginSuccess packet) {
        if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN && !informationForwarded) {
            resultFuture.complete(ConnectionRequestResults.forDisconnect(
                    TextComponent.of("Your server did not send a forwarding request to the proxy. Is it set up correctly?")));
            serverConn.disconnect();
            return true;
        }

        // The player has been logged on to the backend server.
        serverConn.getConnection().setState(StateRegistry.PLAY);
        VelocityServerConnection existingConnection = serverConn.getPlayer().getConnectedServer();
        if (existingConnection == null) {
            // Strap on the play session handler
            serverConn.getPlayer().getConnection().setSessionHandler(new ClientPlaySessionHandler(server, serverConn.getPlayer()));
        } else {
            // The previous server connection should become obsolete.
            // Before we remove it, if the server we are departing is modded, we must always reset the client state.
            if (existingConnection.isLegacyForge()) {
                serverConn.getPlayer().sendLegacyForgeHandshakeResetPacket();
            }
            existingConnection.disconnect();
        }

        resultFuture.complete(ConnectionRequestResults.SUCCESSFUL);
        serverConn.getConnection().setSessionHandler(new BackendPlaySessionHandler(server, serverConn));
        serverConn.getPlayer().setConnectedServer(serverConn);
        return true;
    }

    @Override
    public void exception(Throwable throwable) {
        resultFuture.completeExceptionally(throwable);
    }

    @Override
    public void disconnected() {
        resultFuture.completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
    }

    private static ByteBuf createForwardingData(byte[] hmacSecret, String address, GameProfile profile) {
        ByteBuf dataToForward = Unpooled.buffer();
        ByteBuf finalData = Unpooled.buffer();
        try {
            ProtocolUtils.writeVarInt(dataToForward, VelocityConstants.FORWARDING_VERSION);
            ProtocolUtils.writeString(dataToForward, address);
            ProtocolUtils.writeUuid(dataToForward, profile.idAsUuid());
            ProtocolUtils.writeString(dataToForward, profile.getName());
            ProtocolUtils.writeProperties(dataToForward, profile.getProperties());

            SecretKey key = new SecretKeySpec(hmacSecret, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.update(dataToForward.array(), dataToForward.arrayOffset(), dataToForward.readableBytes());
            byte[] sig = mac.doFinal();
            finalData.writeBytes(sig);
            finalData.writeBytes(dataToForward);
            return finalData;
        } catch (InvalidKeyException e) {
            finalData.release();
            throw new RuntimeException("Unable to authenticate data", e);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            finalData.release();
            throw new AssertionError(e);
        } finally {
            dataToForward.release();
        }
    }
}
