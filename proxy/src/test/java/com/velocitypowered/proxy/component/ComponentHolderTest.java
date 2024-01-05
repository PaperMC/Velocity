/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.component;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

/**
 * ComponentHolder tests.
 */
public class ComponentHolderTest {

  @Test
  void testJsonToBinary() {
    Component component = MiniMessage.miniMessage().deserialize(
        "<#09add3>A <reset><reset>Velocity <#09add3>Server");
    ComponentHolder holder = new ComponentHolder(ProtocolVersion.MINECRAFT_1_20_3, component);
    holder.getJson();
    holder.getBinaryTag();
  }
}
