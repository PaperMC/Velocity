package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StatusResponse implements MinecraftPacket {
    private @Nullable String status;

    public StatusResponse() {}

    public StatusResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        if (status == null) {
            throw new IllegalStateException("Status is not specified");
        }
        return status;
    }

    @Override
    public String toString() {
        return "StatusResponse{" +
                "status='" + status + '\'' +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        status = ProtocolUtils.readString(buf, Short.MAX_VALUE);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        if (status == null) {
            throw new IllegalStateException("Status is not specified");
        }
        ProtocolUtils.writeString(buf, status);
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
