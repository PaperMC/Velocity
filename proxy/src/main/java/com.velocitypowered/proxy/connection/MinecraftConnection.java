package com.velocitypowered.proxy.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.PacketWrapper;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.compression.JavaVelocityCompressor;
import com.velocitypowered.proxy.protocol.encryption.JavaVelocityCipher;
import com.velocitypowered.proxy.protocol.encryption.VelocityCipher;
import com.velocitypowered.proxy.protocol.netty.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.GeneralSecurityException;

import static com.velocitypowered.network.Connections.CIPHER_DECODER;
import static com.velocitypowered.network.Connections.CIPHER_ENCODER;
import static com.velocitypowered.network.Connections.COMPRESSION_DECODER;
import static com.velocitypowered.network.Connections.COMPRESSION_ENCODER;
import static com.velocitypowered.network.Connections.FRAME_DECODER;
import static com.velocitypowered.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.network.Connections.MINECRAFT_ENCODER;

/**
 * A utility class to make working with the pipeline a little less painful and transparently handles certain Minecraft
 * protocol mechanics.
 */
public class MinecraftConnection extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MinecraftConnection.class);

    private final Channel channel;
    private boolean closed;
    private StateRegistry state;
    private MinecraftSessionHandler sessionHandler;
    private int protocolVersion;
    private MinecraftConnectionAssociation association;

    public MinecraftConnection(Channel channel) {
        this.channel = channel;
        this.closed = false;
        this.state = StateRegistry.HANDSHAKE;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (sessionHandler != null) {
            sessionHandler.connected();
        }

        if (association != null) {
            logger.info("{} has connected", association);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (sessionHandler != null) {
            sessionHandler.disconnected();
        }

        if (association != null) {
            logger.info("{} has disconnected", association);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PacketWrapper) {
            PacketWrapper pw = (PacketWrapper) msg;
            try {
                if (sessionHandler != null) {
                    if (pw.getPacket() == null) {
                        sessionHandler.handleUnknown(pw.getBuffer());
                    } else {
                        sessionHandler.handle(pw.getPacket());
                    }
                }
            } finally {
                ReferenceCountUtil.release(pw.getBuffer());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx.channel().isActive()) {
            if (sessionHandler != null) {
                sessionHandler.exception(cause);
            }

            if (association != null) {
                logger.error("{}: exception encountered", association, cause);
            } else {
                logger.error("{} encountered an exception", ctx.channel().remoteAddress(), cause);
            }

            closed = true;
            ctx.close();
        }
    }

    public void write(Object msg) {
        ensureOpen();
        channel.writeAndFlush(msg, channel.voidPromise());
    }

    public void delayedWrite(Object msg) {
        ensureOpen();
        channel.write(msg, channel.voidPromise());
    }

    public void flush() {
        ensureOpen();
        channel.flush();
    }

    public void closeWith(Object msg) {
        ensureOpen();
        teardown();
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
    }

    public void close() {
        ensureOpen();
        teardown();
        channel.close();
    }

    public void teardown() {
        closed = true;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isClosed() {
        return closed;
    }

    public StateRegistry getState() {
        return state;
    }

    public void setState(StateRegistry state) {
        this.state = state;
        this.channel.pipeline().get(MinecraftEncoder.class).setState(state);
        this.channel.pipeline().get(MinecraftDecoder.class).setState(state);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.channel.pipeline().get(MinecraftEncoder.class).setProtocolVersion(protocolVersion);
        this.channel.pipeline().get(MinecraftDecoder.class).setProtocolVersion(protocolVersion);
    }

    public MinecraftSessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public void setSessionHandler(MinecraftSessionHandler sessionHandler) {
        if (this.sessionHandler != null) {
            this.sessionHandler.deactivated();
        }
        this.sessionHandler = sessionHandler;
        sessionHandler.activated();
    }

    private void ensureOpen() {
        Preconditions.checkState(!closed, "Connection is closed.");
    }

    public void setCompressionThreshold(int threshold) {
        if (threshold == -1) {
            channel.pipeline().remove(COMPRESSION_DECODER);
            channel.pipeline().remove(COMPRESSION_ENCODER);
            return;
        }

        JavaVelocityCompressor compressor = new JavaVelocityCompressor();
        MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(threshold, compressor);
        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(threshold, compressor);

        channel.pipeline().addBefore(MINECRAFT_DECODER, COMPRESSION_DECODER, decoder);
        channel.pipeline().addBefore(MINECRAFT_ENCODER, COMPRESSION_ENCODER, encoder);
    }

    public void enableEncryption(byte[] secret) throws GeneralSecurityException {
        SecretKey key = new SecretKeySpec(secret, "AES");

        VelocityCipher decryptionCipher = new JavaVelocityCipher(false, key);
        VelocityCipher encryptionCipher = new JavaVelocityCipher(true, key);
        channel.pipeline().addBefore(FRAME_DECODER, CIPHER_DECODER, new MinecraftCipherDecoder(decryptionCipher));
        channel.pipeline().addBefore(FRAME_ENCODER, CIPHER_ENCODER, new MinecraftCipherEncoder(encryptionCipher));
    }

    public MinecraftConnectionAssociation getAssociation() {
        return association;
    }

    public void setAssociation(MinecraftConnectionAssociation association) {
        this.association = association;
    }
}
