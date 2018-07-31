package com.velocitypowered.proxy.protocol.remap;

import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

public class Minecraft18EntityIdRemapper implements EntityIdRemapper {
    private final int clientId;
    private int serverId;

    public Minecraft18EntityIdRemapper(int clientId, int serverId) {
        this.clientId = clientId;
        this.serverId = serverId;
    }

    @Override
    public ByteBuf remap(ByteBuf original, ProtocolConstants.Direction direction) {
        if (clientId == serverId) {
            // If these are equal (i.e. first connection), no remapping is required.
            return original.retain();
        }

        // TODO: Implement.
        throw new UnsupportedOperationException("1.8 doesn't allow switching servers.");
    }

    @Override
    public int getClientEntityId() {
        return clientId;
    }

    @Override
    public int getServerEntityId() {
        return serverId;
    }

    @Override
    public void setServerEntityId(int id) {
        this.serverId = id;
    }
}
