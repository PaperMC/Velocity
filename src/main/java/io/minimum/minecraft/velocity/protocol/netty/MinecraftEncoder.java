package io.minimum.minecraft.velocity.protocol.netty;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftEncoder extends MessageToByteEncoder<MinecraftPacket> {
    private StateRegistry state;
    private final ProtocolConstants.Direction direction;
    private int protocolVersion;

    public MinecraftEncoder(ProtocolConstants.Direction direction) {
        this.state = StateRegistry.HANDSHAKE;
        this.direction = Preconditions.checkNotNull(direction, "direction");
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MinecraftPacket msg, ByteBuf out) throws Exception {
        StateRegistry.ProtocolMappings mappings = direction == ProtocolConstants.Direction.TO_CLIENT ? state.TO_CLIENT : state.TO_SERVER;
        int packetId = mappings.getId(msg);
        ProtocolUtils.writeVarInt(out, packetId);
        msg.encode(out, direction, protocolVersion);

        System.out.println(direction + " -> " + ByteBufUtil.hexDump(out));
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
