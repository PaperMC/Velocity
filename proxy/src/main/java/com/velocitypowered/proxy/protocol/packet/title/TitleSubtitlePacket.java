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

package com.velocitypowered.proxy.protocol.packet.title;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;

/**
 * The {@code TitleSubtitlePacket} class represents a packet that handles the subtitle content for a title
 * displayed to the player in Minecraft.
 *
 * <p>This packet is used to send the subtitle text that appears below the main title on the player's screen.</p>
 *
 * <p>It extends the {@link GenericTitlePacket}, inheriting basic title properties and focusing
 * on the subtitle content of the title.</p>
 */
public class TitleSubtitlePacket extends GenericTitlePacket {

  private ComponentHolder component;

  public TitleSubtitlePacket() {
    setAction(ActionType.SET_SUBTITLE);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    component.write(buf);
  }

  @Override
  public ComponentHolder getComponent() {
    return component;
  }

  @Override
  public void setComponent(ComponentHolder component) {
    this.component = component;
  }

  @Override
  public String toString() {
    return "TitleSubtitlePacket{"
        + ", component='" + component + '\''
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
