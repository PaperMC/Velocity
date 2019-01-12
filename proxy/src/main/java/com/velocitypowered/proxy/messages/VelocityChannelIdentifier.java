package com.velocitypowered.proxy.messages;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Collection;
import java.util.Optional;

/**
 * A dummy {@link ChannelIdentifier} for communication w/ Velocity
 */
public final class VelocityChannelIdentifier implements ChannelIdentifier {

    private VelocityChannelIdentifier() { }

    private static final byte[] API_MISMATCH = new byte[]{VelocityReplyCodes.API_MISMATCH};
    public static final ChannelIdentifier INSTANCE = new VelocityChannelIdentifier();
    // Internal reference to Velocity
    private static VelocityServer proxy;

    public static void setProxy(VelocityServer proxy) {
        Preconditions.checkState(VelocityChannelIdentifier.proxy == null, "Cannot redefine instance of proxy");
        Preconditions.checkNotNull(proxy, "instance");
        VelocityChannelIdentifier.proxy = proxy;
    }

    @Override
    public String getId() {
        return VelocityChannelConstants.CHANNEL_NAME;
    }

    private static byte[] readFully(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    private static Optional<Player> selectTarget(byte type, ByteBuf data) {
        return (type == PlayerRepresentation.UUID) ?
                proxy.getPlayer(ProtocolUtils.readUuid(data)) :
                proxy.getPlayer(ProtocolUtils.readString(data, 16));
    }

    /**
     * Creates a reply {@link PluginMessage} to the client
     */
    public static PluginMessage createMessage(byte[] dataArray) {
        ByteBuf data = Unpooled.wrappedBuffer(dataArray);
        short api = data.readShort();
        if (api != VelocityChannelConstants.API_VERSION) {
            // Breaking changes?
            PluginMessage response = new PluginMessage();
            response.setChannel(VelocityChannelConstants.CHANNEL_NAME);
            response.setData(API_MISMATCH);
            return response;
        }
        byte[] responseArray;
        ByteBuf response = Unpooled.buffer();
        try {
            byte action = data.readByte();
            switch (action) {
                case VelocityActions.IDENTIFY:
                    ProxyVersion version = proxy.getVersion();
                    response.writeByte(VelocityReplyCodes.SUCCESS);
                    // Write ProxyVersion
                    ProtocolUtils.writeString(response, version.getName());
                    ProtocolUtils.writeString(response, version.getVendor());
                    ProtocolUtils.writeString(response, version.getVendor());
                    break;
                case VelocityActions.SERVER_PLAYERS:
                    // TODO Uncomment me when ready
                    /*
                    // Read players representation type
                    byte type = data.readByte();
                    if (!PlayerRepresentation.isValid(type)) {
                        // Reply with an error
                        response.writeByte(VelocityReplyCodes.UNKNOWN_PLAYER_REPRESENTATION);
                        break;
                    }
                    boolean totalCount = data.readBoolean();
                    if (totalCount) {
                        ProtocolUtils.writeVarInt(response, proxy.getPlayerCount());
                    } else {
                        switch (type) {
                            case PlayerRepresentation.NAME:
                                Set<String> onlineNames = proxy.getOnlineNames();
                                ProtocolUtils.writeVarInt(response, onlineNames.size());
                                for (String name : onlineNames) {
                                    ProtocolUtils.writeString(response, name);
                                }
                                break;
                            case PlayerRepresentation.UUID:
                                Set<UUID> onlineUuids = proxy.getOnlineUuids();
                                ProtocolUtils.writeVarInt(response, onlineUuids.size());
                                for (UUID uuid : onlineUuids) {
                                    ProtocolUtils.writeUuid(response, uuid);
                                }
                                break;
                        }
                    }*/
                    response.writeByte(VelocityReplyCodes.SUCCESS);
                    // Write players count
                    ProtocolUtils.writeVarInt(response, proxy.getPlayerCount());
                    break;
                case VelocityActions.LOCATE: {
                    // Don't know how to extract these duplicates :(
                    byte type = data.readByte();
                    if (!PlayerRepresentation.isValid(type)) {
                        // Reply with an error
                        response.writeByte(VelocityReplyCodes.UNKNOWN_PLAYER_REPRESENTATION);
                        break;
                    }
                    Optional<Player> target = selectTarget(type, data);
                    target.map(p -> {
                        response.writeByte(VelocityReplyCodes.SUCCESS);
                        ProtocolUtils.writeString(response, p.getUsername());
                        ProtocolUtils.writeUuid(response, p.getUniqueId());
                        Optional<ServerConnection> server = p.getCurrentServer();
                        server.map(s -> {
                            response.writeByte(VelocityReplyCodes.SUCCESS);
                            // Write server name
                            ProtocolUtils.writeString(response, s.getServerInfo().getName());
                            return null;
                        }).orElseGet(() -> response.writeByte(VelocityReplyCodes.UNKNOWN_SERVER));
                        return null;
                    }).orElseGet(
                            // We did not locate this player :(
                            () -> response.writeByte(VelocityReplyCodes.UNKNOWN_PLAYER)
                    );
                    break;
                }
                case VelocityActions.CONNECT: {
                    // Don't know how to extract these duplicates :(
                    byte type = data.readByte();
                    if (!PlayerRepresentation.isValid(type)) {
                        // Reply with an error
                        response.writeByte(VelocityReplyCodes.UNKNOWN_PLAYER_REPRESENTATION);
                        break;
                    }
                    Optional<Player> target = selectTarget(type, data);
                    target.map(p -> {
                        String serverName = ProtocolUtils.readString(data);
                        // Try to connect the player
                        // Reply SUCCESS if connected
                        // otherwise UNEXPECTED_ERROR
                        proxy.getServer(serverName)
                                .map(s -> {
                                    p.createConnectionRequest(s).connectWithIndication()
                                            .whenCompleteAsync((b, t) ->
                                                    response.writeByte(b ?
                                                            VelocityReplyCodes.SUCCESS :
                                                            VelocityReplyCodes.UNEXPECTED_ERROR));
                                    return null;
                                })
                                .orElseGet(() -> response.writeByte(VelocityReplyCodes.UNKNOWN_SERVER));
                        return null;
                    }).orElseGet(() -> response.writeByte(VelocityReplyCodes.UNKNOWN_PLAYER));
                    break;
                }
                case VelocityActions.FETCH_SERVERS:
                    response.writeByte(VelocityReplyCodes.SUCCESS);
                    Collection<RegisteredServer> servers = proxy.getAllServers();
                    // Write amount of servers
                    ProtocolUtils.writeVarInt(response, servers.size());
                    // Write servers
                    for (RegisteredServer server : servers) {
                        ProtocolUtils.writeVarInt(response, server.getPlayersConnected().size());
                        ServerInfo info = server.getServerInfo();
                        ProtocolUtils.writeString(response, info.getName());
                    }
                    break;
                case VelocityActions.FORWARD: {
                    byte type = data.readByte();
                    if (!ForwardingType.isValid(type)) {
                        // Reply with an error
                        response.writeByte(VelocityReplyCodes.UNKNOWN_FORWARDING_TYPE);
                        break;
                    }
                    boolean isServer = type == ForwardingType.SERVER;
                    if (isServer || type == ForwardingType.PLAYER) {
                        ChannelMessageSink target;
                        if (!isServer) {
                            // Don't know how to extract these duplicates :(
                            byte representation = data.readByte();
                            if (!PlayerRepresentation.isValid(representation)) {
                                // Reply with an error
                                response.writeByte(VelocityReplyCodes.UNKNOWN_PLAYER_REPRESENTATION);
                                break;
                            }
                            Optional<Player> player = selectTarget(representation, data);
                            if (!player.isPresent()) {
                                response.writeByte(VelocityReplyCodes.UNKNOWN_FORWARDING_SINK);
                                break;
                            } else {
                                target = player.get();
                            }
                        } else {
                            String serverName = ProtocolUtils.readString(data);
                            Optional<RegisteredServer> server = proxy.getServer(serverName);
                            if (!server.isPresent()) {
                                response.writeByte(VelocityReplyCodes.UNKNOWN_FORWARDING_SINK);
                                break;
                            } else {
                                target = server.get();
                            }
                        }
                        byte[] forward = readFully(data);
                        target.sendPluginMessage(INSTANCE, forward);
                        break;
                    }
                    byte[] forward = readFully(data);
                    for (RegisteredServer server : proxy.getAllServers()) {
                        server.sendPluginMessage(VelocityChannelIdentifier.INSTANCE, forward);
                    }
                    response.writeByte(VelocityReplyCodes.SUCCESS);
                    break;
                }
            }
            responseArray = new byte[response.readableBytes()];
            response.readBytes(responseArray);
        } finally {
            response.release();
        }
        PluginMessage message = new PluginMessage();
        message.setChannel(VelocityChannelConstants.CHANNEL_NAME);
        message.setData(responseArray);
        return message;
    }
}
