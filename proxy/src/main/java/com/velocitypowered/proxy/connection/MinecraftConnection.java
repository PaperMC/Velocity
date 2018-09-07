package com.velocitypowered.proxy.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.encryption.VelocityCipherFactory;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.proxy.protocol.netty.*;
import io.netty.buffer.ByteBuf;
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

import static com.velocitypowered.proxy.network.Connections.CIPHER_DECODER;
import static com.velocitypowered.proxy.network.Connections.CIPHER_ENCODER;
import static com.velocitypowered.proxy.network.Connections.COMPRESSION_DECODER;
import static com.velocitypowered.proxy.network.Connections.COMPRESSION_ENCODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;

/**
 * A utility class to make working with the pipeline a little less painful and transparently handles certain Minecraft
 * protocol mechanics.
 */
public class MinecraftConnection extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MinecraftConnection.class);

    private final Channel channel;
    private StateRegistry state;
    private MinecraftSessionHandler sessionHandler;
    private int protocolVersion;
    private MinecraftConnectionAssociation association;
    private boolean isLegacyForge;
    private final VelocityServer server;
    private boolean canSendLegacyFMLResetPacket = false;

    public MinecraftConnection(Channel channel, VelocityServer server) {
        this.channel = channel;
        this.server = server;
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
        if (msg instanceof MinecraftPacket) {
            sessionHandler.handle((MinecraftPacket) msg);
        } else if (msg instanceof ByteBuf) {
            try {
                sessionHandler.handleUnknown((ByteBuf) msg);
            } finally {
                ReferenceCountUtil.release(msg);
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

            ctx.close();
        }
    }

    public void write(Object msg) {
        if (channel.isActive()) {
            channel.writeAndFlush(msg, channel.voidPromise());
        }
    }

    public void delayedWrite(Object msg) {
        if (channel.isActive()) {
            channel.write(msg, channel.voidPromise());
        }
    }

    public void flush() {
        if (channel.isActive()) {
            channel.flush();
        }
    }

    public void closeWith(Object msg) {
        if (channel.isActive()) {
            channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void close() {
        if (channel.isActive()) {
            channel.close();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isClosed() {
        return !channel.isActive();
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
        if (protocolVersion != ProtocolConstants.LEGACY) {
            this.channel.pipeline().get(MinecraftEncoder.class).setProtocolVersion(protocolVersion);
            this.channel.pipeline().get(MinecraftDecoder.class).setProtocolVersion(protocolVersion);
        } else {
            // Legacy handshake handling
            this.channel.pipeline().remove(MINECRAFT_ENCODER);
            this.channel.pipeline().remove(MINECRAFT_DECODER);
        }
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
        Preconditions.checkState(!isClosed(), "Connection is closed.");
    }

    public void setCompressionThreshold(int threshold) {
        ensureOpen();

        if (threshold == -1) {
            channel.pipeline().remove(COMPRESSION_DECODER);
            channel.pipeline().remove(COMPRESSION_ENCODER);
            return;
        }

        int level = server.getConfiguration().getCompressionLevel();
        VelocityCompressor compressor = Natives.compressor.get().create(level);
        MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(threshold, compressor);
        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(threshold, compressor);

        channel.pipeline().addBefore(MINECRAFT_DECODER, COMPRESSION_DECODER, decoder);
        channel.pipeline().addBefore(MINECRAFT_ENCODER, COMPRESSION_ENCODER, encoder);
    }

    public void enableEncryption(byte[] secret) throws GeneralSecurityException {
        ensureOpen();

        SecretKey key = new SecretKeySpec(secret, "AES");

        VelocityCipherFactory factory = Natives.cipher.get();
        VelocityCipher decryptionCipher = factory.forDecryption(key);
        VelocityCipher encryptionCipher = factory.forEncryption(key);
        channel.pipeline().addBefore(FRAME_DECODER, CIPHER_DECODER, new MinecraftCipherDecoder(decryptionCipher));
        channel.pipeline().addBefore(FRAME_ENCODER, CIPHER_ENCODER, new MinecraftCipherEncoder(encryptionCipher));
    }

    public MinecraftConnectionAssociation getAssociation() {
        return association;
    }

    public void setAssociation(MinecraftConnectionAssociation association) {
        this.association = association;
    }

    public boolean isLegacyForge() {
        return isLegacyForge;
    }

    public void setLegacyForge(boolean isForge) {
        this.isLegacyForge = isForge;
    }

    public boolean canSendLegacyFMLResetPacket() {
        return canSendLegacyFMLResetPacket;
    }

    public void setCanSendLegacyFMLResetPacket(boolean canSendLegacyFMLResetPacket) {
        this.canSendLegacyFMLResetPacket = isLegacyForge && canSendLegacyFMLResetPacket;
    }
}
