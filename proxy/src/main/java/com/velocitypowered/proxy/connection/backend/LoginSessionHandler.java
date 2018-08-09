package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.data.GameProfile;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import net.kyori.text.TextComponent;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;
    private ScheduledFuture<?> forwardingCheckTask;

    public LoginSessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void activated() {
        if (VelocityServer.getServer().getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN) {
            forwardingCheckTask = connection.getMinecraftConnection().getChannel().eventLoop().schedule(() -> {
                connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(),
                        TextComponent.of("Your server did not send the forwarding request in time. Is it set up correctly?"));
            }, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof EncryptionRequest) {
            throw new IllegalStateException("Backend server is online-mode!");
        } else if (packet instanceof LoginPluginMessage) {
            LoginPluginMessage message = (LoginPluginMessage) packet;
            VelocityConfiguration configuration = VelocityServer.getServer().getConfiguration();
            if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN &&
                    message.getChannel().equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {
                LoginPluginResponse response = new LoginPluginResponse();
                response.setSuccess(true);
                response.setId(message.getId());
                response.setData(createForwardingData(configuration.getForwardingSecret(),
                        connection.getProxyPlayer().getRemoteAddress().getHostString(),
                        connection.getProxyPlayer().getProfile()));
                connection.getMinecraftConnection().write(response);
                cancelForwardingCheck();

                ServerLogin login = new ServerLogin();
                login.setUsername(connection.getProxyPlayer().getUsername());
                connection.getMinecraftConnection().write(login);
            } else {
                // Don't understand
                LoginPluginResponse response = new LoginPluginResponse();
                response.setSuccess(false);
                response.setId(message.getId());
                response.setData(Unpooled.EMPTY_BUFFER);
                connection.getMinecraftConnection().write(response);
            }
        } else if (packet instanceof Disconnect) {
            Disconnect disconnect = (Disconnect) packet;
            connection.disconnect();

            // Do we have an outstanding notification? If so, fulfill it.
            doNotify(ConnectionRequestResults.forDisconnect(disconnect));

            connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), disconnect);
        } else if (packet instanceof SetCompression) {
            SetCompression sc = (SetCompression) packet;
            connection.getMinecraftConnection().setCompressionThreshold(sc.getThreshold());
        } else if (packet instanceof ServerLoginSuccess) {
            // The player has been logged on to the backend server.
            connection.getMinecraftConnection().setState(StateRegistry.PLAY);
            ServerConnection existingConnection = connection.getProxyPlayer().getConnectedServer();
            if (existingConnection == null) {
                // Strap on the play session handler
                connection.getProxyPlayer().getConnection().setSessionHandler(new ClientPlaySessionHandler(connection.getProxyPlayer()));
            } else {
                // The previous server connection should become obsolete.
                existingConnection.disconnect();
            }

            // Do we have an outstanding notification? If so, fulfill it.
            doNotify(ConnectionRequestResults.SUCCESSFUL);

            connection.getMinecraftConnection().setSessionHandler(new BackendPlaySessionHandler(connection));
            connection.getProxyPlayer().setConnectedServer(connection);
        }
    }

    @Override
    public void deactivated() {
        cancelForwardingCheck();
    }

    @Override
    public void exception(Throwable throwable) {
        connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), throwable);
    }

    private void doNotify(ConnectionRequestBuilder.Result result) {
        ChannelPipeline pipeline = connection.getMinecraftConnection().getChannel().pipeline();
        ServerConnection.ConnectionNotifier n = pipeline.get(ServerConnection.ConnectionNotifier.class);
        if (n != null) {
            n.getResult().complete(result);
            pipeline.remove(ServerConnection.ConnectionNotifier.class);
        }
    }

    private void cancelForwardingCheck() {
        if (forwardingCheckTask != null) {
            forwardingCheckTask.cancel(false);
            forwardingCheckTask = null;
        }
    }

    static ByteBuf createForwardingData(byte[] hmacSecret, String address, GameProfile profile) {
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
