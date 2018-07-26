package com.velocitypowered.proxy.protocol.compression;

import com.velocitypowered.proxy.util.Disposable;
import io.netty.buffer.ByteBuf;

import java.util.zip.DataFormatException;

public interface VelocityCompressor extends Disposable {
    void inflate(ByteBuf source, ByteBuf destination) throws DataFormatException;

    void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;
}
