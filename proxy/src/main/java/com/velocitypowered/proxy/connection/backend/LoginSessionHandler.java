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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
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
    private boolean informationForwarded;

    public LoginSessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
        this.server = server;
        this.serverConn = serverConn;
    }

    @Override
    public void handleGeneric(MinecraftPacket packet) {
        if (packet instanceof EncryptionRequest) {
            throw new IllegalStateException("Backend server is online-mode!");
        } else if (packet instanceof LoginPluginMessage) {
            LoginPluginMessage message = (LoginPluginMessage) packet;
            VelocityConfiguration configuration = server.getConfiguration();
            if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN &&
                    message.getChannel().equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {
                LoginPluginResponse response = new LoginPluginResponse();
                response.setSuccess(true);
                response.setId(message.getId());
                response.setData(createForwardingData(configuration.getForwardingSecret(),
                        serverConn.getPlayer().getRemoteAddress().getHostString(),
                        serverConn.getPlayer().getProfile()));
                serverConn.getConnection().write(response);
                informationForwarded = true;
            } else {
                // Don't understand
                LoginPluginResponse response = new LoginPluginResponse();
                response.setSuccess(false);
                response.setId(message.getId());
                response.setData(Unpooled.EMPTY_BUFFER);
                serverConn.getConnection().write(response);
            }
        } else if (packet instanceof Disconnect) {
            Disconnect disconnect = (Disconnect) packet;
            // Do we have an outstanding notification? If so, fulfill it.
            doNotify(ConnectionRequestResults.forDisconnect(disconnect));
            serverConn.disconnect();
        } else if (packet instanceof SetCompression) {
            SetCompression sc = (SetCompression) packet;
            serverConn.getConnection().setCompressionThreshold(sc.getThreshold());
        } else if (packet instanceof ServerLoginSuccess) {
            if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN && !informationForwarded) {
                doNotify(ConnectionRequestResults.forDisconnect(
                        TextComponent.of("Your server did not send a forwarding request to the proxy. Is it set up correctly?")));
                serverConn.disconnect();
                return;
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

            doNotify(ConnectionRequestResults.SUCCESSFUL);
            serverConn.getConnection().setSessionHandler(new BackendPlaySessionHandler(server, serverConn));
            serverConn.getPlayer().setConnectedServer(serverConn);
        }
    }

    @Override
    public void exception(Throwable throwable) {
        CompletableFuture<ConnectionRequestBuilder.Result> future = serverConn.getConnection().getChannel()
                .attr(VelocityServerConnection.CONNECTION_NOTIFIER).getAndSet(null);
        if (future != null) {
            future.completeExceptionally(throwable);
        }
    }

    @Override
    public void disconnected() {
        CompletableFuture<ConnectionRequestBuilder.Result> future = serverConn.getConnection().getChannel()
                .attr(VelocityServerConnection.CONNECTION_NOTIFIER).getAndSet(null);
        if (future != null) {
            future.completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
        }
    }

    private void doNotify(ConnectionRequestBuilder.Result result) {
        CompletableFuture<ConnectionRequestBuilder.Result> future = serverConn.getConnection().getChannel()
                .attr(VelocityServerConnection.CONNECTION_NOTIFIER).getAndSet(null);
        if (future != null) {
            future.complete(result);
        }
    }

    private static ByteBuf createForwardingData(byte[] hmacSecret, String address, GameProfile profile) {
        ByteBuf dataToForward = Unpooled.buffer();
        ByteBuf finalData = Unpooled.buffer();
        try {
            ProtocolUtils.writeString(dataToForward, address);
            ProtocolUtils.writeUuid(dataToForward, profile.idAsUuid());
            ProtocolUtils.writeString(dataToForward, profile.getName());
            ProtocolUtils.writeVarInt(dataToForward, profile.getProperties().size());
            for (GameProfile.Property property : profile.getProperties()) {
                ProtocolUtils.writeString(dataToForward, property.getName());
                ProtocolUtils.writeString(dataToForward, property.getValue());
                String signature = property.getSignature();
                if (signature != null) {
                    dataToForward.writeBoolean(true);
                    ProtocolUtils.writeString(dataToForward, signature);
                } else {
                    dataToForward.writeBoolean(false);
                }
            }

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
