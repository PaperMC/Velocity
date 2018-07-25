package com.velocitypowered.proxy.protocol.compression;

import io.netty.buffer.ByteBuf;

import java.util.zip.DataFormatException;

public interface VelocityCompressor {
    void inflate(ByteBuf source, ByteBuf destination) throws DataFormatException;

    void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;

    void dispose();
}
