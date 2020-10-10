package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.kyori.text.TextComponent;

public class LoginSessionHandler implements MinecraftSessionHandler {

  private static final TextComponent MODERN_IP_FORWARDING_FAILURE = TextComponent
      .of("Your server did not send a forwarding request to the proxy. Is it set up correctly?");

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Impl> resultFuture;
  private boolean informationForwarded;

  LoginSessionHandler(VelocityServer server, VelocityServerConnection serverConn,
      CompletableFuture<Impl> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;
  }

  @Override
  public boolean handle(EncryptionRequest packet) {
    throw new IllegalStateException("Backend server is online-mode!");
  }

  @Override
  public boolean handle(LoginPluginMessage packet) {
    MinecraftConnection mc = serverConn.ensureConnected();
    VelocityConfiguration configuration = server.getConfiguration();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN && packet
        .getChannel()
        .equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {
      LoginPluginResponse response = new LoginPluginResponse();
      response.setSuccess(true);
      response.setId(packet.getId());
      response.setData(createForwardingData(configuration.getForwardingSecret(),
          cleanRemoteAddress(serverConn.getPlayer().getRemoteAddress()),
          serverConn.getPlayer().getGameProfile()));
      mc.write(response);
      informationForwarded = true;
    } else {
      // Don't understand
      LoginPluginResponse response = new LoginPluginResponse();
      response.setSuccess(false);
      response.setId(packet.getId());
      response.setData(Unpooled.EMPTY_BUFFER);
      mc.write(response);
    }
    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    serverConn.disconnect();
    return true;
  }

  @Override
  public boolean handle(SetCompression packet) {
    serverConn.ensureConnected().setCompressionThreshold(packet.getThreshold());
    return true;
  }

  @Override
  public boolean handle(ServerLoginSuccess packet) {
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && !informationForwarded) {
      resultFuture.complete(ConnectionRequestResults.forDisconnect(MODERN_IP_FORWARDING_FAILURE,
          serverConn.getServer()));
      serverConn.disconnect();
      return true;
    }

    // The player has been logged on to the backend server, but we're not done yet. There could be
    // other problems that could arise before we get a JoinGame packet from the server.

    // Move into the PLAY phase.
    MinecraftConnection smc = serverConn.ensureConnected();
    smc.setState(StateRegistry.PLAY);

    // Switch to the transition handler.
    smc.setSessionHandler(new TransitionSessionHandler(server, serverConn, resultFuture));
    return true;
  }

  @Override
  public void exception(Throwable throwable) {
    resultFuture.completeExceptionally(throwable);
  }

  @Override
  public void disconnected() {
    resultFuture
        .completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
  }

  private static String cleanRemoteAddress(InetSocketAddress address) {
    String addressString = address.getAddress().getHostAddress();
    int ipv6ScopeIdx = addressString.indexOf('%');
    if (ipv6ScopeIdx == -1) {
      return addressString;
    } else {
      return addressString.substring(0, ipv6ScopeIdx);
    }
  }

  private static ByteBuf createForwardingData(byte[] hmacSecret, String address,
      GameProfile profile) {
    ByteBuf dataToForward = Unpooled.buffer();
    ByteBuf finalData = Unpooled.buffer();
    try {
      ProtocolUtils.writeVarInt(dataToForward, VelocityConstants.FORWARDING_VERSION);
      ProtocolUtils.writeString(dataToForward, address);
      ProtocolUtils.writeUuid(dataToForward, profile.getId());
      ProtocolUtils.writeString(dataToForward, profile.getName());
      ProtocolUtils.writeProperties(dataToForward, profile.getProperties());

      SecretKey key = new SecretKeySpec(hmacSecret, "HmacSHA256");
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      mac.update(dataToForward.array(), dataToForward.arrayOffset(), dataToForward.readableBytes());
      byte[] sig = mac.doFinal();
      finalData.writeBytes(sig);
      finalData.writeBytes(dataToForward);
      return finalData;
    } catch (InvalidKeyException e) {
      finalData.release();
      throw new RuntimeException("Unable to authenticate data", e);
    } catch (NoSuchAlgorithmException e) {
      // Should never happen
      finalData.release();
      throw new AssertionError(e);
    } finally {
      dataToForward.release();
    }
  }
}
