package com.velocitypowered.proxy.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.StatusRequestPacket;
import com.velocitypowered.proxy.protocol.packet.StatusResponsePacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class PingSessionHandler implements MinecraftSessionHandler {

  private final CompletableFuture<ServerPing> result;
  private final RegisteredServer server;
  private final MinecraftConnection connection;
  private final ProtocolVersion version;
  private boolean completed = false;

  PingSessionHandler(CompletableFuture<ServerPing> result, RegisteredServer server,
      MinecraftConnection connection, ProtocolVersion version) {
    this.result = result;
    this.server = server;
    this.connection = connection;
    this.version = version;
  }

  @Override
  public void activated() {
    HandshakePacket handshake = new HandshakePacket();
    handshake.setNextStatus(StateRegistry.STATUS_ID);

    SocketAddress address = server.getServerInfo().getAddress();
    if (address instanceof InetSocketAddress) {
      InetSocketAddress socketAddr = (InetSocketAddress) address;
      handshake.setServerAddress(socketAddr.getHostString());
      handshake.setPort(socketAddr.getPort());
    } else {
      // Just fake it
      handshake.setServerAddress("127.0.0.1");
    }
    handshake.setProtocolVersion(version);
    connection.delayedWrite(handshake);

    connection.setState(StateRegistry.STATUS);
    connection.delayedWrite(StatusRequestPacket.INSTANCE);

    connection.flush();
  }

  @Override
  public boolean handle(StatusResponsePacket packet) {
    // All good!
    completed = true;
    connection.close(true);

    ServerPing ping = VelocityServer.getPingGsonInstance(version).fromJson(packet.getStatus(),
        ServerPing.class);
    result.complete(ping);
    return true;
  }

  @Override
  public void disconnected() {
    if (!completed) {
      result.completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
    }
  }

  @Override
  public void exception(Throwable throwable) {
    completed = true;
    result.completeExceptionally(throwable);
  }
}
