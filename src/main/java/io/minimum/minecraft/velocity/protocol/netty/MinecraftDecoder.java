package io.minimum.minecraft.velocity.protocol.netty;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf> {
    private StateRegistry state;
    private final ProtocolConstants.Direction direction;
    private int protocolVersion;

    public MinecraftDecoder(ProtocolConstants.Direction direction) {
        this.state = StateRegistry.HANDSHAKE;
        this.direction = Preconditions.checkNotNull(direction, "direction");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (msg.isReadable()) {
            return;
        }

        ByteBuf slice = msg.slice().retain();

        int packetId = ProtocolUtils.readVarInt(msg);
        StateRegistry.ProtocolMappings mappings = direction == ProtocolConstants.Direction.TO_CLIENT ? state.TO_CLIENT : state.TO_SERVER;
        MinecraftPacket packet = mappings.createPacket(packetId);
        System.out.println("Decode!");
        System.out.println("packet ID: " + packetId);
        System.out.println("packet hexdump: " + ByteBufUtil.hexDump(slice));
        if (packet == null) {
            msg.skipBytes(msg.readableBytes());
            out.add(new PacketWrapper(null, slice));
        } else {
            packet.decode(msg, direction, protocolVersion);
            out.add(new PacketWrapper(packet, slice));
        }
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public StateRegistry getState() {
        return state;
    }

    public void setState(StateRegistry state) {
        this.state = state;
    }

    public ProtocolConstants.Direction getDirection() {
        return direction;
    }
}
