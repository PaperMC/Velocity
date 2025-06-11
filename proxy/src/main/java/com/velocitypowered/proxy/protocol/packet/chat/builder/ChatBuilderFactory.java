/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat.builder;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatBuilder;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatBuilder;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionChatBuilder;
import java.util.function.Function;

public class ChatBuilderFactory {

  private final ProtocolVersion version;
  private final Function<ProtocolVersion, ChatBuilderV2> builderFunction;

  public ChatBuilderFactory(ProtocolVersion version) {
    this.version = version;
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      this.builderFunction = SessionChatBuilder::new;
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      this.builderFunction = KeyedChatBuilder::new;
    } else {
      this.builderFunction = LegacyChatBuilder::new;
    }
  }

  public ChatBuilderV2 builder() {
    return this.builderFunction.apply(this.version);
  }
}
