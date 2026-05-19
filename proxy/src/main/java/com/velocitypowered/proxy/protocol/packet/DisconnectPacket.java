/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The clientbound packet that disconnects the client, carrying the reason shown to the player.
 */
public class DisconnectPacket implements MinecraftPacket {

  private @Nullable ComponentHolder reason;
  private final StateRegistry state;

  public DisconnectPacket(StateRegistry state) {
    this.state = state;
  }

  private DisconnectPacket(StateRegistry state, ComponentHolder reason) {
    this.state = state;
    this.reason = Preconditions.checkNotNull(reason, "reason");
  }

  /**
   * Returns the disconnect reason.
   *
   * @return the reason component holder
   * @throws IllegalStateException if no reason has been set
   */
  public ComponentHolder getReason() {
    if (reason == null) {
      throw new IllegalStateException("No reason specified");
    }
    return reason;
  }

  public void setReason(@Nullable ComponentHolder reason) {
    this.reason = reason;
  }

  @Override
  public String toString() {
    return "Disconnect{"
        + "reason='" + reason + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    reason = ComponentHolder.read(buf, state == StateRegistry.LOGIN
          ? ProtocolVersion.MINECRAFT_1_20_2 : version);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    getReason().write(buf);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  /**
   * Creates a disconnect packet for the given reason, serialized for the supplied protocol
   * version and connection state.
   *
   * @param component the reason to show the player
   * @param version the protocol version to serialize for
   * @param state the connection state the packet is sent in
   * @return the disconnect packet
   */
  public static DisconnectPacket create(Component component, ProtocolVersion version, StateRegistry state) {
    Preconditions.checkNotNull(component, "component");
    return new DisconnectPacket(state, new ComponentHolder(state == StateRegistry.LOGIN
            ? ProtocolVersion.MINECRAFT_1_20_2 : version, component));
  }
}
