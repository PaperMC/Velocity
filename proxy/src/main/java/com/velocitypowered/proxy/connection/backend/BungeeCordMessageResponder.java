/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.messages.PairedPluginChannelId;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.buffer.ByteBufDataInput;
import com.velocitypowered.proxy.network.buffer.ByteBufDataOutput;
import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundPluginMessagePacket;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.StringJoiner;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "Most methods in this class open "
    + "instances of ByteBufDataOutput backed by heap-allocated ByteBufs. Closing them does "
    + "nothing.")
public class BungeeCordMessageResponder {

  private static final PairedPluginChannelId CHANNEL = PluginChannelId
      .withLegacy("BungeeCord", Key.key("bungeecord", "main"));

  private final VelocityServer proxy;
  private final ConnectedPlayer player;

  BungeeCordMessageResponder(VelocityServer proxy, ConnectedPlayer player) {
    this.proxy = proxy;
    this.player = player;
  }

  public static boolean isBungeeCordMessage(AbstractPluginMessagePacket<?> message) {
    return CHANNEL.modernChannelKey().asString().equals(message.getChannel())
        || CHANNEL.legacyChannel().equals(message.getChannel());
  }

  private void processConnect(ByteBufDataInput in) {
    String serverName = in.readUTF();
    RegisteredServer server = proxy.server(serverName);
    if (server != null) {
      player.createConnectionRequest(server).fireAndForget();
    }
  }

  private void processConnectOther(ByteBufDataInput in) {
    String playerName = in.readUTF();
    String serverName = in.readUTF();

    Player referencedPlayer = proxy.player(playerName);
    RegisteredServer referencedServer = proxy.server(serverName);
    if (referencedPlayer != null && referencedServer != null) {
      referencedPlayer.createConnectionRequest(referencedServer).fireAndForget();
    }
  }

  private void processIp() {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);
    out.writeUTF("IP");

    sendIpOutput(player, buf, out);
  }

  private void processPlayerCount(ByteBufDataInput in) {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    String target = in.readUTF();
    if (target.equals("ALL")) {
      out.writeUTF("PlayerCount");
      out.writeUTF("ALL");
      out.writeInt(proxy.countConnectedPlayers());
    } else {
      RegisteredServer referencedServer = proxy.server(target);
      if (referencedServer != null) {
        int playersOnServer = referencedServer.connectedPlayers().size();
        out.writeUTF("PlayerCount");
        out.writeUTF(referencedServer.serverInfo().name());
        out.writeInt(playersOnServer);
      }
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
      for (Player online : proxy.connectedPlayers()) {
        joiner.add(online.username());
      }
      out.writeUTF(joiner.toString());
    } else {
      RegisteredServer referencedServer = proxy.server(target);
      if (referencedServer != null) {
        out.writeUTF("PlayerList");
        out.writeUTF(referencedServer.serverInfo().name());

        StringJoiner joiner = new StringJoiner(", ");
        for (Player online : referencedServer.connectedPlayers()) {
          joiner.add(online.username());
        }
        out.writeUTF(joiner.toString());
      }
    }

    if (buf.isReadable()) {
      sendResponseOnConnection(buf);
    } else {
      buf.release();
    }
  }

  private void processGetServers() {
    StringJoiner joiner = new StringJoiner(", ");
    for (RegisteredServer server : proxy.registeredServers()) {
      joiner.add(server.serverInfo().name());
    }

    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);
    out.writeUTF("GetServers");
    out.writeUTF(joiner.toString());

    sendResponseOnConnection(buf);
  }

  private void processMessage(ByteBufDataInput in) {
    processMessage0(in, LegacyComponentSerializer.legacySection());
  }

  private void processMessageRaw(ByteBufDataInput in) {
    processMessage0(in, GsonComponentSerializer.gson());
  }

  private void processMessage0(ByteBufDataInput in,
      ComponentSerializer<Component, ?, String> serializer) {
    String target = in.readUTF();
    String message = in.readUTF();

    Component messageComponent = serializer.deserialize(message);
    if (target.equals("ALL")) {
      proxy.sendMessage(Identity.nil(), messageComponent);
    } else {
      Player player = proxy.player(target);
      if (player != null) {
        player.sendMessage(Identity.nil(), messageComponent);
      }
    }
  }

  private void processGetServer() {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    out.writeUTF("GetServer");
    out.writeUTF(player.ensureAndGetCurrentServer().serverInfo().name());

    sendResponseOnConnection(buf);
  }

  private void processUuid() {
    ByteBuf buf = Unpooled.buffer();
    ByteBufDataOutput out = new ByteBufDataOutput(buf);

    out.writeUTF("UUID");
    out.writeUTF(UuidUtils.toUndashed(player.id()));

    sendResponseOnConnection(buf);
  }

  private void processUuidOther(ByteBufDataInput in) {
    Player player = proxy.player(in.readUTF());
    if (player != null) {
      ByteBuf buf = Unpooled.buffer();
      ByteBufDataOutput out = new ByteBufDataOutput(buf);

      out.writeUTF("UUIDOther");
      out.writeUTF(player.username());
      out.writeUTF(UuidUtils.toUndashed(player.id()));

      sendResponseOnConnection(buf);
    }
  }

  private void processIpOther(ByteBufDataInput in) {
    Player player = proxy.player(in.readUTF());
    if (player != null) {
      ByteBuf buf = Unpooled.buffer();
      ByteBufDataOutput out = new ByteBufDataOutput(buf);

      out.writeUTF("IPOther");
      out.writeUTF(player.username());
      sendIpOutput(player, buf, out);
    }
  }

  private void sendIpOutput(Player player, ByteBuf buf, ByteBufDataOutput out) {
    SocketAddress address = player.remoteAddress();
    if (address instanceof InetSocketAddress) {
      InetSocketAddress serverInetAddr = (InetSocketAddress) address;
      out.writeUTF(serverInetAddr.getHostString());
      out.writeInt(serverInetAddr.getPort());
    } else {
      out.writeUTF("127.0.0.1");
      out.writeInt(0);
    }

    sendResponseOnConnection(buf);
  }

  private void processServerIp(ByteBufDataInput in) {
    RegisteredServer info = proxy.server(in.readUTF());
    if (info != null) {
      ByteBuf buf = Unpooled.buffer();
      ByteBufDataOutput out = new ByteBufDataOutput(buf);

      out.writeUTF("ServerIP");
      out.writeUTF(info.serverInfo().name());
      SocketAddress address = info.serverInfo().address();
      if (address instanceof InetSocketAddress) {
        InetSocketAddress serverInetAddr = (InetSocketAddress) address;
        out.writeUTF(serverInetAddr.getHostString());
        out.writeShort(serverInetAddr.getPort());
      } else {
        out.writeUTF("unix://" + ((DomainSocketAddress) address).path());
        out.writeShort(0);
      }

      sendResponseOnConnection(buf);
    }
  }

  private void processKick(ByteBufDataInput in) {
    Player player = proxy.player(in.readUTF());
    if (player != null) {
      String kickReason = in.readUTF();
      player.disconnect(LegacyComponentSerializer.legacySection().deserialize(kickReason));
    }
  }

  private void processForwardToPlayer(ByteBufDataInput in) {
    Player player = proxy.player(in.readUTF());
    if (player != null) {
      ByteBuf toForward = in.unwrap().copy();
      sendServerResponse((ConnectedPlayer) player, toForward);
    }
  }

  private void processForwardToServer(ByteBufDataInput in) {
    String target = in.readUTF();
    ByteBuf toForward = in.unwrap().copy();
    if (target.equals("ALL")) {
      try {
        for (RegisteredServer rs : proxy.registeredServers()) {
          ((VelocityRegisteredServer) rs).sendPluginMessage(CHANNEL,
              toForward.retainedSlice());
        }
      } finally {
        toForward.release();
      }
    } else {
      RegisteredServer server = proxy.server(target);
      if (server != null) {
        ((VelocityRegisteredServer) server).sendPluginMessage(CHANNEL, toForward);
      } else {
        toForward.release();
      }
    }
  }

  static String getBungeeCordChannel(ProtocolVersion version) {
    return version.gte(ProtocolVersion.MINECRAFT_1_13) ? CHANNEL.modernChannelKey().asString()
        : CHANNEL.legacyChannel();
  }

  // Note: this method will always release the buffer!
  private void sendResponseOnConnection(ByteBuf buf) {
    sendServerResponse(this.player, buf);
  }

  // Note: this method will always release the buffer!
  private static void sendServerResponse(ConnectedPlayer player, ByteBuf buf) {
    MinecraftConnection serverConnection = player.ensureAndGetCurrentServer().ensureConnected();
    String chan = getBungeeCordChannel(serverConnection.getProtocolVersion());
    ServerboundPluginMessagePacket msg = new ServerboundPluginMessagePacket(chan, buf);
    serverConnection.write(msg);
  }

  boolean process(AbstractPluginMessagePacket<?> message) {
    if (!proxy.configuration().isBungeePluginChannelEnabled()) {
      return false;
    }

    if (!isBungeeCordMessage(message)) {
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
        this.processIp();
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
      case "MessageRaw":
        this.processMessageRaw(in);
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
      case "IPOther":
        this.processIpOther(in);
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
