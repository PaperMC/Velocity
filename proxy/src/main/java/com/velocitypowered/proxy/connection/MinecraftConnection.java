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
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipherFactory;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
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
import io.netty.handler.timeout.ReadTimeoutException;
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
  private ProtocolVersion protocolVersion;
  private ProtocolVersion nextProtocolVersion;
  private @Nullable MinecraftConnectionAssociation association;
  private final VelocityServer server;
  private ConnectionType connectionType = ConnectionTypes.UNDETERMINED;
  private boolean knownDisconnect = false;

  /**
   * Initializes a new {@link MinecraftConnection} instance.
   * @param channel the channel on the connection
   * @param server the Velocity instance
   */
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

    if (association != null && !knownDisconnect) {
      logger.info("{} has disconnected", association);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    try {
      if (sessionHandler == null) {
        // No session handler available, do nothing
        return;
      }

      if (sessionHandler.beforeHandle()) {
        return;
      }

      if (msg instanceof MinecraftPacket) {
        MinecraftPacket pkt = (MinecraftPacket) msg;
        if (!pkt.handle(sessionHandler)) {
          sessionHandler.handleGeneric((MinecraftPacket) msg);
        }
      } else if (msg instanceof HAProxyMessage) {
        HAProxyMessage proxyMessage = (HAProxyMessage) msg;
        this.remoteAddress = new InetSocketAddress(proxyMessage.sourceAddress(),
            proxyMessage.sourcePort());
      } else if (msg instanceof ByteBuf) {
        sessionHandler.handleUnknown((ByteBuf) msg);
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (ctx.channel().isActive()) {
      if (sessionHandler != null) {
        try {
          sessionHandler.exception(cause);
        } catch (Exception ex) {
          logger.error("{}: exception handling exception", (association != null ? association :
              channel.remoteAddress()), cause);
        }
      }

      if (association != null) {
        if (cause instanceof ReadTimeoutException) {
          logger.error("{}: read timed out", association);
        } else {
          logger.error("{}: exception encountered", association, cause);
        }
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

  /**
   * Writes and immediately flushes a message to the connection.
   * @param msg the message to write
   */
  public void write(Object msg) {
    if (channel.isActive()) {
      channel.writeAndFlush(msg, channel.voidPromise());
    }
  }

  /**
   * Writes, but does not flush, a message to the connection.
   * @param msg the message to write
   */
  public void delayedWrite(Object msg) {
    if (channel.isActive()) {
      channel.write(msg, channel.voidPromise());
    }
  }

  /**
   * Flushes the connection.
   */
  public void flush() {
    if (channel.isActive()) {
      channel.flush();
    }
  }

  /**
   * Closes the connection after writing the {@code msg}.
   * @param msg the message to write
   */
  public void closeWith(Object msg) {
    if (channel.isActive()) {
      knownDisconnect = true;
      channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * Immediately closes the connection.
   */
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

  public boolean isAutoReading() {
    return channel.config().isAutoRead();
  }

  /**
   * Determines whether or not the channel should continue reading data automaticaly.
   * @param autoReading whether or not we should read data automatically
   */
  public void setAutoReading(boolean autoReading) {
    channel.config().setAutoRead(autoReading);
    if (autoReading) {
      // For some reason, the channel may not completely read its queued contents once autoread
      // is turned back on, even though toggling autoreading on should handle things automatically.
      // We will issue an explicit read after turning on autoread.
      //
      // Much thanks to @creeper123123321.
      channel.read();
    }
  }

  /**
   * Changes the state of the Minecraft connection.
   * @param state the new state
   */
  public void setState(StateRegistry state) {
    this.state = state;
    this.channel.pipeline().get(MinecraftEncoder.class).setState(state);
    this.channel.pipeline().get(MinecraftDecoder.class).setState(state);
  }

  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  /**
   * Sets the new protocol version for the connection.
   * @param protocolVersion the protocol version to use
   */
  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
    this.nextProtocolVersion = protocolVersion;
    if (protocolVersion != ProtocolVersion.LEGACY) {
      this.channel.pipeline().get(MinecraftEncoder.class).setProtocolVersion(protocolVersion);
      this.channel.pipeline().get(MinecraftDecoder.class).setProtocolVersion(protocolVersion);
    } else {
      // Legacy handshake handling
      this.channel.pipeline().remove(MINECRAFT_ENCODER);
      this.channel.pipeline().remove(MINECRAFT_DECODER);
    }
  }

  public @Nullable MinecraftSessionHandler getSessionHandler() {
    return sessionHandler;
  }

  /**
   * Sets the session handler for this connection.
   * @param sessionHandler the handler to use
   */
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

  /**
   * Sets the compression threshold on the connection. You are responsible for sending
   * {@link com.velocitypowered.proxy.protocol.packet.SetCompression} beforehand.
   * @param threshold the compression threshold to use
   */
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

  /**
   * Enables encryption on the connection.
   * @param secret the secret key negotiated between the client and the server
   * @throws GeneralSecurityException if encryption can't be enabled
   */
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

  public @Nullable MinecraftConnectionAssociation getAssociation() {
    return association;
  }

  public void setAssociation(MinecraftConnectionAssociation association) {
    this.association = association;
  }

  public ProtocolVersion getNextProtocolVersion() {
    return this.nextProtocolVersion;
  }

  public void setNextProtocolVersion(ProtocolVersion nextProtocolVersion) {
    this.nextProtocolVersion = nextProtocolVersion;
  }

  /**
   * Gets the detected {@link ConnectionType}.
   * @return The {@link ConnectionType}
   */
  public ConnectionType getType() {
    return connectionType;
  }

  /**
   * Sets the detected {@link ConnectionType}.
   * @param connectionType The {@link ConnectionType}
   */
  public void setType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }
}
