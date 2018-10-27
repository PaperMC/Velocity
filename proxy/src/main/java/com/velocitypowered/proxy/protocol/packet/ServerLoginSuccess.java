package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public class ServerLoginSuccess implements MinecraftPacket {
    private @Nullable UUID uuid;
    private @Nullable String username;

    public UUID getUuid() {
        if (uuid == null) {
            throw new IllegalStateException("No UUID specified!");
        }
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        if (username == null) {
            throw new IllegalStateException("No username specified!");
        }
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "ServerLoginSuccess{" +
                "uuid=" + uuid +
                ", username='" + username + '\'' +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        uuid = UUID.fromString(ProtocolUtils.readString(buf, 36));
        username = ProtocolUtils.readString(buf, 16);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        if (uuid == null) {
            throw new IllegalStateException("No UUID specified!");
        }
        ProtocolUtils.writeString(buf, uuid.toString());
        if (username == null) {
            throw new IllegalStateException("No username specified!");
        }
        ProtocolUtils.writeString(buf, username);
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
