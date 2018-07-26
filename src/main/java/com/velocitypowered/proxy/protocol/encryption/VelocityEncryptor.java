package com.velocitypowered.proxy.protocol.encryption;

import com.velocitypowered.proxy.util.Disposable;
import io.netty.buffer.ByteBuf;

public interface VelocityEncryptor extends Disposable {
    void process(ByteBuf source, ByteBuf destination);
}
