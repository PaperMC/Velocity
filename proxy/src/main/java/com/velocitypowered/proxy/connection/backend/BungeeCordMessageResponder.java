package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.ByteBufDataInput;
import com.velocitypowered.proxy.protocol.util.ByteBufDataOutput;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.StringJoiner;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

class BungeeCordMessageResponder {

  private static final MinecraftChannelIdentifier MODERN_CHANNEL = MinecraftChannelIdentifier
      .create("bungeecord", "main");
  private static final LegacyChannelIdentifier LEGACY_CHANNEL =
      new LegacyChannelIdentifier("BungeeCord");

  private final VelocityServer proxy;
  private final ConnectedPlayer player;

  BungeeCordMessageResponder(VelocityServer proxy, ConnectedPlayer player) {
    this.proxy = proxy;
    this.player = player;
  }

  private void processConnect(ByteBufDataInput in) {
    String serverName = in.readUTF();
    proxy.getServer(serverName).ifPresent(server -> player.createConnectionRequest(server)
        .fireAndForget());
  }

  private void processConnectOther(ByteBufDataInput in) {
    String playerName = in.readUTF();
    String serverName = in.readUTF();

    proxy.getPlayer(playerName).flatMap(player -> proxy.getServer(serverName))
        .ifPresent(server -> player.createConnectionRequest(server).fireAndForget());
  }

  private void processIp(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);
    out.writeUTF("IP");
    out.writeUTF(player.getRemoteAddress().getHostString());
    out.writeInt(player.getRemoteAddress().getPort());
    sendResponseOnConnection(buf);
  }

  private void processPlayerCount(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    String target = in.readUTF();
    if (target.equals("ALL")) {
      out.writeUTF("PlayerCount");
      out.writeUTF("ALL");
      out.writeInt(proxy.getPlayerCount());
    } else {
      proxy.getServer(target).ifPresent(rs -> {
        int playersOnServer = rs.getPlayersConnected().size();
        out.writeUTF("PlayerCount");
        out.writeUTF(rs.getServerInfo().getName());
        out.writeInt(playersOnServer);
      });
    }

    if (buf.isReadable()) {
      sendResponseOnConnection(buf);
    } else {
      buf.release();
    }
  }

  private void processPlayerList(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    String target = in.readUTF();
    if (target.equals("ALL")) {
      out.writeUTF("PlayerList");
      out.writeUTF("ALL");

      StringJoiner joiner = new StringJoiner(", ");
      for (Player online : proxy.getAllPlayers()) {
        joiner.add(online.getUsername());
      }
      out.writeUTF(joiner.toString());
    } else {
      proxy.getServer(target).ifPresent(info -> {
        out.writeUTF("PlayerList");
        out.writeUTF(info.getServerInfo().getName());

        StringJoiner joiner = new StringJoiner(", ");
        for (Player online : info.getPlayersConnected()) {
          joiner.add(online.getUsername());
        }
        out.writeUTF(joiner.toString());
      });
    }

    if (buf.isReadable()) {
      sendResponseOnConnection(buf);
    } else {
      buf.release();
    }
  }

  private void processGetServers() {
    StringJoiner joiner = new StringJoiner(", ");
    for (RegisteredServer server : proxy.getAllServers()) {
      joiner.add(server.getServerInfo().getName());
    }

    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);
    out.writeUTF("GetServers");
    out.writeUTF(joiner.toString());

    sendResponseOnConnection(buf);
  }

  private void processMessage(ByteBufDataInput in) {
    String target = in.readUTF();
    String message = in.readUTF();
    if (target.equals("ALL")) {
      for (Player player : proxy.getAllPlayers()) {
        player.sendMessage(LegacyComponentSerializer.INSTANCE.deserialize(message));
      }
    } else {
      proxy.getPlayer(target).ifPresent(player -> {
        player.sendMessage(LegacyComponentSerializer.INSTANCE.deserialize(message));
      });
    }
  }

  private void processGetServer() {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    out.writeUTF("GetServer");
    out.writeUTF(player.ensureAndGetCurrentServer().getServerInfo().getName());

    sendResponseOnConnection(buf);
  }

  private void processUuid() {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    out.writeUTF("UUID");
    out.writeUTF(UuidUtils.toUndashed(player.getUniqueId()));

    sendResponseOnConnection(buf);
  }

  private void processUuidOther(ByteBufDataInput in) {
    proxy.getPlayer(in.readUTF()).ifPresent(player -> {
      ByteBuf buf = Unpooled.buffer();
      ByteBufDataOutput out = new ByteBufDataOutput(buf);

      out.writeUTF("UUIDOther");
      out.writeUTF(player.getUsername());
      out.writeUTF(UuidUtils.toUndashed(player.getUniqueId()));

      sendResponseOnConnection(buf);
    });
  }

  private void processServerIp(ByteBufDataInput in) {
    proxy.getServer(in.readUTF()).ifPresent(info -> {
      ByteBuf buf = Unpooled.buffer();
      ByteBufDataOutput out = new ByteBufDataOutput(buf);

      out.writeUTF("ServerIP");
      out.writeUTF(info.getServerInfo().getName());
      out.writeUTF(info.getServerInfo().getAddress().getHostString());
      out.writeShort(info.getServerInfo().getAddress().getPort());

      sendResponseOnConnection(buf);
    });
  }

  private void processKick(ByteBufDataInput in) {
    proxy.getPlayer(in.readUTF()).ifPresent(player -> {
      String kickReason = in.readUTF();
      player.disconnect(LegacyComponentSerializer.INSTANCE.deserialize(kickReason));
    });
  }

  private ByteBuf prepareForwardMessage(ByteBufDataInput in) {
    String channel = in.readUTF();
    short messageLength = in.readShort();

    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput forwarded = new ByteBufDataOutput(buf);
    forwarded.writeUTF(channel);
    forwarded.writeShort(messageLength);
    buf.writeBytes(in.unwrap().readSlice(messageLength));
    return buf;
  }

  private void processForwardToPlayer(ByteBufDataInput in) {
    proxy.getPlayer(in.readUTF())
        .flatMap(Player::getCurrentServer)
        .ifPresent(server -> sendServerResponse(player, prepareForwardMessage(in)));
  }

  private void processForwardToServer(ByteBufDataInput in) {
    ByteBuf toForward = prepareForwardMessage(in);
    String target = in.readUTF();
    if (target.equals("ALL")) {
      ByteBuf unreleasableForward = Unpooled.unreleasableBuffer(toForward);
      try {
        for (RegisteredServer rs : proxy.getAllServers()) {
          ((VelocityRegisteredServer) rs).sendPluginMessage(LEGACY_CHANNEL, unreleasableForward);
        }
      } finally {
        toForward.release();
      }
    } else {
      proxy.getServer(target).ifPresent(rs -> ((VelocityRegisteredServer) rs)
          .sendPluginMessage(LEGACY_CHANNEL, toForward));
    }
  }

  // Note: this method will always release the buffer!
  private void sendResponseOnConnection(ByteBuf buf) {
    sendServerResponse(this.player, buf);
  }

  static String getBungeeCordChannel(ProtocolVersion version) {
    return version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0 ? MODERN_CHANNEL.getId()
        : LEGACY_CHANNEL.getId();
  }

  // Note: this method will always release the buffer!
  private static void sendServerResponse(ConnectedPlayer player, ByteBuf buf) {
    MinecraftConnection serverConnection = player.ensureAndGetCurrentServer().ensureConnected();
    String chan = getBungeeCordChannel(serverConnection.getProtocolVersion());

    PluginMessage msg = null;
    boolean released = false;

    try {
      VelocityServerConnection vsc = player.getConnectedServer();
      if (vsc == null) {
        return;
      }

      MinecraftConnection serverConn = vsc.ensureConnected();
      msg = new PluginMessage(chan, buf);
      serverConn.write(msg);
      released = true;
    } finally {
      if (!released && msg != null) {
        msg.release();
      }
    }
  }

  boolean process(PluginMessage message) {
    if (!MODERN_CHANNEL.getId().equals(message.getChannel()) && !LEGACY_CHANNEL.getId()
        .equals(message.getChannel())) {
      return false;
    }

    ByteBufDataInput in = new ByteBufDataInput(message.content());
    String subChannel = in.readUTF();
    switch (subChannel) {
      case "ForwardToPlayer":
        this.processForwardToPlayer(in);
        break;
      case "Forward":
        this.processForwardToServer(in);
        break;
      case "Connect":
        this.processConnect(in);
        break;
      case "ConnectOther":
        this.processConnectOther(in);
        break;
      case "IP":
        this.processIp(in);
        break;
      case "PlayerCount":
        this.processPlayerCount(in);
        break;
      case "PlayerList":
        this.processPlayerList(in);
        break;
      case "GetServers":
        this.processGetServers();
        break;
      case "Message":
        this.processMessage(in);
        break;
      case "GetServer":
        this.processGetServer();
        break;
      case "UUID":
        this.processUuid();
        break;
      case "UUIDOther":
        this.processUuidOther(in);
        break;
      case "ServerIP":
        this.processServerIp(in);
        break;
      case "KickPlayer":
        this.processKick(in);
        break;
      default:
        // Do nothing, unknown command
        break;
    }

    return true;
  }
}
