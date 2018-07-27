package com.velocitypowered.proxy.protocol.encryption;

import com.velocitypowered.proxy.util.Disposable;
import io.netty.buffer.ByteBuf;

import javax.crypto.ShortBufferException;

public interface VelocityCipher extends Disposable {
    void process(ByteBuf source, ByteBuf destination) throws ShortBufferException;
}
