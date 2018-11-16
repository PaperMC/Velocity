package com.velocitypowered.proxy.connection;

import static com.velocitypowered.proxy.network.Connections.CIPHER_DECODER;
import static com.velocitypowered.proxy.network.Connections.CIPHER_ENCODER;
import static com.velocitypowered.proxy.network.Connections.COMPRESSION_DECODER;
import static com.velocitypowered.proxy.network.Connections.COMPRESSION_ENCODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipherFactory;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftCipherDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftCipherEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A utility class to make working with the pipeline a little less painful and transparently handles
 * certain Minecraft protocol mechanics.
 */
public class MinecraftConnection extends ChannelInboundHandlerAdapter {

  private static final Logger logger = LogManager.getLogger(MinecraftConnection.class);

  private final Channel channel;
  private SocketAddress remoteAddress;
  private StateRegistry state;
  private @Nullable MinecraftSessionHandler sessionHandler;
  private int protocolVersion;
  private int nextProtocolVersion;
  private @Nullable MinecraftConnectionAssociation association;
  private boolean isLegacyForge;
  private final VelocityServer server;
  private boolean canSendLegacyFmlResetPacket = false;

  public MinecraftConnection(Channel channel, VelocityServer server) {
    this.channel = channel;
    this.remoteAddress = channel.remoteAddress();
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
    if (sessionHandler == null) {
      // No session handler available, do nothing
      ReferenceCountUtil.release(msg);
      return;
    }

    if (msg instanceof MinecraftPacket) {
      if (sessionHandler.beforeHandle()) {
        return;
      }

      MinecraftPacket pkt = (MinecraftPacket) msg;
      if (!pkt.handle(sessionHandler)) {
        sessionHandler.handleGeneric((MinecraftPacket) msg);
      }
    } else if (msg instanceof HAProxyMessage) {
      if (sessionHandler.beforeHandle()) {
        return;
      }

      HAProxyMessage proxyMessage = (HAProxyMessage) msg;
      this.remoteAddress = new InetSocketAddress(proxyMessage.sourceAddress(),
          proxyMessage.sourcePort());
    } else if (msg instanceof ByteBuf) {
      try {
        if (sessionHandler.beforeHandle()) {
          return;
        }
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
      }

      ctx.close();
    }
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    if (sessionHandler != null) {
      sessionHandler.writabilityChanged();
    }
  }

  public EventLoop eventLoop() {
    return channel.eventLoop();
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

  public SocketAddress getRemoteAddress() {
    return remoteAddress;
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
    this.nextProtocolVersion = protocolVersion;
    if (protocolVersion != ProtocolConstants.LEGACY) {
      this.channel.pipeline().get(MinecraftEncoder.class).setProtocolVersion(protocolVersion);
      this.channel.pipeline().get(MinecraftDecoder.class).setProtocolVersion(protocolVersion);
    } else {
      // Legacy handshake handling
      this.channel.pipeline().remove(MINECRAFT_ENCODER);
      this.channel.pipeline().remove(MINECRAFT_DECODER);
    }
  }

  @Nullable
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
    VelocityCompressor compressor = Natives.compress.get().create(level);
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
    channel.pipeline()
        .addBefore(FRAME_DECODER, CIPHER_DECODER, new MinecraftCipherDecoder(decryptionCipher));
    channel.pipeline()
        .addBefore(FRAME_ENCODER, CIPHER_ENCODER, new MinecraftCipherEncoder(encryptionCipher));
  }

  @Nullable
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

  public boolean canSendLegacyFmlResetPacket() {
    return canSendLegacyFmlResetPacket;
  }

  public void setCanSendLegacyFmlResetPacket(boolean canSendLegacyFMLResetPacket) {
    this.canSendLegacyFmlResetPacket = isLegacyForge && canSendLegacyFMLResetPacket;
  }

  public int getNextProtocolVersion() {
    return this.nextProtocolVersion;
  }

  public void setNextProtocolVersion(int nextProtocolVersion) {
    this.nextProtocolVersion = nextProtocolVersion;
  }
}
