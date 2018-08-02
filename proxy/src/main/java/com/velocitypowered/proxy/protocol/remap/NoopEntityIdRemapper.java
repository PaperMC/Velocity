package com.velocitypowered.proxy.protocol.remap;

import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

public class NoopEntityIdRemapper implements EntityIdRemapper {
    public static final NoopEntityIdRemapper INSTANCE = new NoopEntityIdRemapper();

    private NoopEntityIdRemapper() {

    }

    @Override
    public ByteBuf remap(ByteBuf original, ProtocolConstants.Direction direction) {
        return original.retain();
    }

    @Override
    public int getClientEntityId() {
        return 0;
    }

    @Override
    public int getServerEntityId() {
        return 0;
    }

    @Override
    public void setServerEntityId(int id) {

    }
}
