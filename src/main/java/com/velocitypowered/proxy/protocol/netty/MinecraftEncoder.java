package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftEncoder extends MessageToByteEncoder<MinecraftPacket> {
    private StateRegistry state;
    private final ProtocolConstants.Direction direction;
    private StateRegistry.PacketRegistry.ProtocolVersion protocolVersion;

    public MinecraftEncoder(ProtocolConstants.Direction direction) {
        this.state = StateRegistry.HANDSHAKE;
        this.direction = Preconditions.checkNotNull(direction, "direction");
        this.setProtocolVersion(ProtocolConstants.MINIMUM_VERSION_SUPPORTED);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MinecraftPacket msg, ByteBuf out) {
        int packetId = this.protocolVersion.getPacketId(msg);
        ProtocolUtils.writeVarInt(out, packetId);
        msg.encode(out, direction, protocolVersion.id);
    }

    public StateRegistry.PacketRegistry.ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(final int protocolVersion) {
        this.protocolVersion = (this.direction == ProtocolConstants.Direction.CLIENTBOUND ? this.state.CLIENTBOUND : this.state.SERVERBOUND).getVersion(protocolVersion);
    }

    public StateRegistry getState() {
        return state;
    }

    public void setState(StateRegistry state) {
        this.state = state;
        this.setProtocolVersion(protocolVersion.id);
    }

    public ProtocolConstants.Direction getDirection() {
        return direction;
    }
}
