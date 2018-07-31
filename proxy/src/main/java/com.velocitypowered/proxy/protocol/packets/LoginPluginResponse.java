package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class LoginPluginResponse implements MinecraftPacket {
    private int id;
    private boolean success;
    private ByteBuf data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "LoginPluginResponse{" +
                "id=" + id +
                ", success=" + success +
                ", data=" + data +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.id = ProtocolUtils.readVarInt(buf);
        this.success = buf.readBoolean();
        if (buf.isReadable()) {
            this.data = buf.readRetainedSlice(buf.readableBytes());
        } else {
            this.data = Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeVarInt(buf, id);
        buf.writeBoolean(success);
        buf.writeBytes(data);
    }
}
