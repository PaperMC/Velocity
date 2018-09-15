package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.util.ThrowableUtils;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;
import net.kyori.text.serializer.PlainComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConnectedPlayer implements MinecraftConnectionAssociation, Player {
    private static final PlainComponentSerializer PASS_THRU_TRANSLATE = new PlainComponentSerializer((c) -> "", TranslatableComponent::key);
    public static final PermissionProvider DEFAULT_PERMISSIONS = s -> PermissionFunction.ALWAYS_UNDEFINED;

    private static final Logger logger = LogManager.getLogger(ConnectedPlayer.class);

    private final MinecraftConnection connection;
    private final InetSocketAddress virtualHost;
    private final GameProfile profile;
    private PermissionFunction permissionFunction = null;
    private int tryIndex = 0;
    private long ping = -1;
    private VelocityServerConnection connectedServer;
    private VelocityServerConnection connectionInFlight;
    private PlayerSettings settings;
    private final VelocityServer server;
    
    public ConnectedPlayer(VelocityServer server, GameProfile profile, MinecraftConnection connection, InetSocketAddress virtualHost) {
        this.server = server;
        this.profile = profile;
        this.connection = connection;
        this.virtualHost = virtualHost;
    }

    @Override
    public String getUsername() {
        return profile.getName();
    }

    @Override
    public UUID getUniqueId() {
        return profile.idAsUuid();
    }

    @Override
    public Optional<ServerConnection> getCurrentServer() {
        return Optional.ofNullable(connectedServer);
    }

    public GameProfile getProfile() {
        return profile;
    }

    public MinecraftConnection getConnection() {
        return connection;
    }

    @Override
    public long getPing() {
        return this.ping;
    }
    
    public void setPing(long ping) {
        this.ping = ping;
    }
    
    public PlayerSettings getPlayerSettings() {
        return settings == null ? ClientSettingsWrapper.DEFAULT : this.settings;
    }

    public void setPlayerSettings(ClientSettings settings) {
        this.settings = new ClientSettingsWrapper(settings);
        server.getEventManager().fireAndForget(new PlayerSettingsChangedEvent(this, this.settings));
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) connection.getChannel().remoteAddress();
    }

    @Override
    public Optional<InetSocketAddress> getVirtualHost() {
        return Optional.ofNullable(virtualHost);
    }

    public void setPermissionFunction(PermissionFunction permissionFunction) {
        this.permissionFunction = permissionFunction;
    }

    @Override
    public boolean isActive() {
        return connection.getChannel().isActive();
    }

    @Override
    public int getProtocolVersion() {
        return connection.getProtocolVersion();
    }

    @Override
    public void sendMessage(@NonNull Component component, @NonNull MessagePosition position) {
        Preconditions.checkNotNull(component, "component");
        Preconditions.checkNotNull(position, "position");

        byte pos = (byte) position.ordinal();
        String json;
        if (position == MessagePosition.ACTION_BAR) {
            // Due to issues with action bar packets, we'll need to convert the text message into a legacy message
            // and then inject the legacy text into a component... yuck!
            JsonObject object = new JsonObject();
            object.addProperty("text", ComponentSerializers.LEGACY.serialize(component));
            json = VelocityServer.GSON.toJson(object);
        } else {
            json = ComponentSerializers.JSON.serialize(component);
        }

        Chat chat = new Chat();
        chat.setType(pos);
        chat.setMessage(json);
        connection.write(chat);
    }

    @Override
    public ConnectionRequestBuilder createConnectionRequest(@NonNull ServerInfo info) {
        return new ConnectionRequestBuilderImpl(info);
    }

    @Override
    public void setHeaderAndFooter(@NonNull Component header, @NonNull Component footer) {
        Preconditions.checkNotNull(header, "header");
        Preconditions.checkNotNull(footer, "footer");
        connection.write(HeaderAndFooter.create(header, footer));
    }

    @Override
    public void clearHeaderAndFooter() {
        connection.write(HeaderAndFooter.reset());
    }
    
    @Override
    public void disconnect(Component reason) {
        connection.closeWith(Disconnect.create(reason));
    }

    public VelocityServerConnection getConnectedServer() {
        return connectedServer;
    }

    public void handleConnectionException(ServerInfo info, Throwable throwable) {
        String error = ThrowableUtils.briefDescription(throwable);
        String userMessage;
        if (connectedServer != null && connectedServer.getServerInfo().equals(info)) {
            userMessage = "Exception in server " + info.getName();
        } else {
            logger.error("{}: unable to connect to server {}", this, info.getName(), throwable);
            userMessage = "Exception connecting to server " + info.getName();
        }
        handleConnectionException(info, null, TextComponent.builder()
                .content(userMessage + ": ")
                .color(TextColor.RED)
                .append(TextComponent.of(error, TextColor.WHITE))
                .build());
    }

    public void handleConnectionException(ServerInfo info, Disconnect disconnect) {
        Component disconnectReason = ComponentSerializers.JSON.deserialize(disconnect.getReason());
        String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
        if (connectedServer != null && connectedServer.getServerInfo().equals(info)) {
            logger.error("{}: kicked from server {}: {}", this, info.getName(), plainTextReason);
        } else {
            logger.error("{}: disconnected while connecting to {}: {}", this, info.getName(), plainTextReason);
        }
        handleConnectionException(info, disconnectReason, TextComponent.builder()
                .content("Unable to connect to " + info.getName() + ": ")
                .color(TextColor.RED)
                .append(disconnectReason)
                .build());
    }

    private void handleConnectionException(ServerInfo info, @Nullable Component kickReason, Component friendlyReason) {
        boolean alreadyConnected = connectedServer != null && connectedServer.getServerInfo().equals(info);;
        connectionInFlight = null;
        if (connectedServer == null) {
            // The player isn't yet connected to a server.
            Optional<ServerInfo> nextServer = getNextServerToTry();
            if (nextServer.isPresent()) {
                createConnectionRequest(nextServer.get()).fireAndForget();
            } else {
                connection.closeWith(Disconnect.create(friendlyReason));
            }
        } else if (connectedServer.getServerInfo().equals(info)) {
            // Already connected to the server being disconnected from.
            if (kickReason != null) {
                server.getEventManager().fire(new KickedFromServerEvent(this, info, kickReason, !alreadyConnected, friendlyReason))
                        .thenAcceptAsync(event -> {
                            if (event.getResult() instanceof KickedFromServerEvent.DisconnectPlayer) {
                                KickedFromServerEvent.DisconnectPlayer res = (KickedFromServerEvent.DisconnectPlayer) event.getResult();
                                connection.closeWith(Disconnect.create(res.getReason()));
                            } else if (event.getResult() instanceof KickedFromServerEvent.RedirectPlayer) {
                                KickedFromServerEvent.RedirectPlayer res = (KickedFromServerEvent.RedirectPlayer) event.getResult();
                                createConnectionRequest(res.getServer()).fireAndForget();
                            } else {
                                // In case someone gets creative, assume we want to disconnect the player.
                                connection.closeWith(Disconnect.create(friendlyReason));
                            }
                        }, connection.getChannel().eventLoop());
            } else {
                connection.closeWith(Disconnect.create(friendlyReason));
            }
        } else {
            connection.write(Chat.create(friendlyReason));
        }
    }

    Optional<ServerInfo> getNextServerToTry() {
        List<String> serversToTry = server.getConfiguration().getAttemptConnectionOrder();
        if (tryIndex >= serversToTry.size()) {
            return Optional.empty();
        }

        String toTryName = serversToTry.get(tryIndex);
        tryIndex++;
        return server.getServers().getServer(toTryName);
    }

    private CompletableFuture<ConnectionRequestBuilder.Result> connect(ConnectionRequestBuilderImpl request) {
        if (connectionInFlight != null) {
            return CompletableFuture.completedFuture(
                    ConnectionRequestResults.plainResult(ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS)
            );
        }

        if (connectedServer != null && connectedServer.getServerInfo().equals(request.getServer())) {
            return CompletableFuture.completedFuture(
                    ConnectionRequestResults.plainResult(ConnectionRequestBuilder.Status.ALREADY_CONNECTED)
            );
        }

        // Otherwise, initiate the connection.
        ServerPreConnectEvent event = new ServerPreConnectEvent(this, request.getServer());
        return server.getEventManager().fire(event)
                .thenCompose((newEvent) -> {
                    if (!newEvent.getResult().isAllowed()) {
                        return CompletableFuture.completedFuture(
                                ConnectionRequestResults.plainResult(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED)
                        );
                    }

                    return new VelocityServerConnection(newEvent.getResult().getServer().get(), this, server).connect();
                });
    }

    public void setConnectedServer(VelocityServerConnection serverConnection) {
        if (this.connectedServer != null && !serverConnection.getServerInfo().equals(connectedServer.getServerInfo())) {
            this.tryIndex = 0;
        }
        this.connectedServer = serverConnection;
    }

    public void sendLegacyForgeHandshakeResetPacket() {
        if (connection.canSendLegacyFMLResetPacket()) {
            PluginMessage resetPacket = new PluginMessage();
            resetPacket.setChannel(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL);
            resetPacket.setData(VelocityConstants.FORGE_LEGACY_HANDSHAKE_RESET_DATA);
            connection.write(resetPacket);
            connection.setCanSendLegacyFMLResetPacket(false);
        }
    }

    public void close(TextComponent reason) {
        connection.closeWith(Disconnect.create(reason));
    }

    public void teardown() {
        if (connectionInFlight != null) {
            connectionInFlight.disconnect();
        }
        if (connectedServer != null) {
            connectedServer.disconnect();
        }
        server.unregisterConnection(this);
    }

    @Override
    public String toString() {
        return "[connected player] " + getProfile().getName() + " (" + getRemoteAddress() + ")";
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissionFunction.getPermissionSetting(permission).asBoolean();
    }

    @Override
    public void sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(data, "data");
        PluginMessage message = new PluginMessage();
        message.setChannel(identifier.getId());
        message.setData(data);
        connection.write(message);
    }

    private class ConnectionRequestBuilderImpl implements ConnectionRequestBuilder {
        private final ServerInfo info;

        ConnectionRequestBuilderImpl(ServerInfo info) {
            this.info = Preconditions.checkNotNull(info, "info");
        }

        @Override
        public ServerInfo getServer() {
            return info;
        }

        @Override
        public CompletableFuture<Result> connect() {
            return ConnectedPlayer.this.connect(this);
        }

        @Override
        public void fireAndForget() {
            connect()
                    .whenCompleteAsync((status, throwable) -> {
                        if (throwable != null) {
                            handleConnectionException(info, throwable);
                            return;
                        }

                        switch (status.getStatus()) {
                            case ALREADY_CONNECTED:
                                sendMessage(ConnectionMessages.ALREADY_CONNECTED);
                                break;
                            case CONNECTION_IN_PROGRESS:
                                sendMessage(ConnectionMessages.IN_PROGRESS);
                                break;
                            case CONNECTION_CANCELLED:
                                // Ignored; the plugin probably already handled this.
                                break;
                            case SERVER_DISCONNECTED:
                                handleConnectionException(info, Disconnect.create(status.getReason().orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR)));
                                break;
                        }
                    }, connection.getChannel().eventLoop());
        }
    }
}
