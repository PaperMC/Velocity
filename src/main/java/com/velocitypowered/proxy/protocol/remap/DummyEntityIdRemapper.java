package com.velocitypowered.proxy.protocol.remap;

import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

public class DummyEntityIdRemapper implements EntityIdRemapper {
    public static final DummyEntityIdRemapper INSTANCE = new DummyEntityIdRemapper();

    private DummyEntityIdRemapper() {

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
