package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.StringJoiner;
import java.util.stream.Collectors;
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

    proxy.getPlayer(playerName).ifPresent(player -> {
      proxy.getServer(serverName).ifPresent(server -> player.createConnectionRequest(server)
          .fireAndForget());
    });
  }

  private void processIp(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);
    out.writeUTF("IP");
    out.writeUTF(player.getRemoteAddress().getHostString());
    out.writeInt(player.getRemoteAddress().getPort());
    sendResponse(buf);
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
      sendResponse(buf);
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
      sendResponse(buf);
    } else {
      buf.release();
    }
  }

  private void processGetServers(ByteBufDataInput in) {
    StringJoiner joiner = new StringJoiner(", ");
    for (RegisteredServer server : proxy.getAllServers()) {
      joiner.add(server.getServerInfo().getName());
    }

    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);
    out.writeUTF("GetServers");
    out.writeUTF(joiner.toString());

    sendResponse(buf);
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

  private void processGetServer(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    out.writeUTF("GetServer");
    out.writeUTF(player.ensureAndGetCurrentServer().getServerInfo().getName());

    sendResponse(buf);
  }

  private void processUuid(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    out.writeUTF("UUID");
    out.writeUTF(UuidUtils.toUndashed(player.getUniqueId()));

    sendResponse(buf);
  }

  private void processUuidOther(ByteBufDataInput in) {
    proxy.getPlayer(in.readUTF()).ifPresent(player -> {
      ByteBuf buf = Unpooled.buffer();
      ByteBufDataOutput out = new ByteBufDataOutput(buf);

      out.writeUTF("UUIDOther");
      out.writeUTF(player.getUsername());
      out.writeUTF(UuidUtils.toUndashed(player.getUniqueId()));

      sendResponse(buf);
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

      sendResponse(buf);
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
        .ifPresent(server -> sendResponse(player, prepareForwardMessage(in)));
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
  private void sendResponse(ByteBuf buf) {
    sendResponse(this.player, buf);
  }

  // Note: this method will always release the buffer!
  private static void sendResponse(ConnectedPlayer player, ByteBuf buf) {
    MinecraftConnection serverConnection = player.ensureAndGetCurrentServer().ensureConnected();
    String chan = serverConnection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13)
        >= 0 ? MODERN_CHANNEL.getId() : LEGACY_CHANNEL.getId();

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

  public boolean process(PluginMessage message) {
    if (!MODERN_CHANNEL.getId().equals(message.getChannel()) && !LEGACY_CHANNEL.getId()
        .equals(message.getChannel())) {
      return false;
    }

    ByteBufDataInput in = new ByteBufDataInput(message.content());
    String subChannel = in.readUTF();
    if (subChannel.equals("ForwardToPlayer")) {
      this.processForwardToPlayer(in);
    }
    if (subChannel.equals("Forward")) {
      this.processForwardToServer(in);
    }
    if (subChannel.equals("Connect")) {
      this.processConnect(in);
    }
    if (subChannel.equals("ConnectOther")) {
      this.processConnectOther(in);
    }
    if (subChannel.equals("IP")) {
      this.processIp(in);
    }
    if (subChannel.equals("PlayerCount")) {
      this.processPlayerCount(in);
    }
    if (subChannel.equals("PlayerList")) {
      this.processPlayerList(in);
    }
    if (subChannel.equals("GetServers")) {
      this.processGetServers(in);
    }
    if (subChannel.equals("Message")) {
      this.processMessage(in);
    }
    if (subChannel.equals("GetServer")) {
      this.processGetServer(in);
    }
    if (subChannel.equals("UUID")) {
      this.processUuid(in);
    }
    if (subChannel.equals("UUIDOther")) {
      this.processUuidOther(in);
    }
    if (subChannel.equals("ServerIP")) {
      this.processServerIp(in);
    }
    if (subChannel.equals("KickPlayer")) {
      this.processKick(in);
    }

    return true;
  }
}
