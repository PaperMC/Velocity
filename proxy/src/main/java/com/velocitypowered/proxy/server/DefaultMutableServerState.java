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

package com.velocitypowered.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.MutableServerState;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DefaultMutableServerState implements MutableServerState, ForwardingAudience {
  private final Map<UUID, ConnectedPlayer> players = new ConcurrentHashMap<>();

  @Override
  public Collection<Player> getPlayersConnected() {
    return ImmutableList.copyOf(players.values());
  }

  @Override
  public void addPlayer(Player player) {
    Preconditions.checkState(player instanceof ConnectedPlayer, "player is not a proxy player");
    players.put(player.getUniqueId(), (ConnectedPlayer) player);
  }

  @Override
  public void removePlayer(Player player) {
    Preconditions.checkState(player instanceof ConnectedPlayer, "player is not a proxy player");
    players.remove(player.getUniqueId(), player);
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    return sendPluginMessage(identifier, Unpooled.wrappedBuffer(data));
  }

  /**
   * Sends a plugin message to the server through this connection. The message will be released
   * afterwards.
   *
   * @param identifier the channel ID to use
   * @param data the data
   * @return whether or not the message was sent
   */
  public boolean sendPluginMessage(ChannelIdentifier identifier, ByteBuf data) {
    for (ConnectedPlayer player : players.values()) {
      VelocityServerConnection connection = player.getConnectedServer();
      if (connection != null && connection.getServer().getState() == this) {
        return connection.sendPluginMessage(identifier, data);
      }
    }

    data.release();
    return false;
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return this.getPlayersConnected();
  }
}
