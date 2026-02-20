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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a remote chat session that implements the {@link ChatSession} interface.
 * This session is used for handling chat interactions that occur remotely, typically between
 * a client and server, allowing for communication and session tracking.
 */
public class RemoteChatSession implements ChatSession {

  private final @Nullable UUID sessionId;
  private final IdentifiedKey identifiedKey;

  public RemoteChatSession(ProtocolVersion version, ByteBuf buf) {
    this.sessionId = ProtocolUtils.readUuid(buf);
    this.identifiedKey = ProtocolUtils.readPlayerKey(version, buf);
  }

  public RemoteChatSession(@Nullable UUID sessionId, IdentifiedKey identifiedKey) {
    this.sessionId = sessionId;
    this.identifiedKey = identifiedKey;
  }

  public IdentifiedKey getIdentifiedKey() {
    return identifiedKey;
  }

  public @Nullable UUID getSessionId() {
    return sessionId;
  }

  public void write(ByteBuf buf) {
    ProtocolUtils.writeUuid(buf, Objects.requireNonNull(this.sessionId));
    ProtocolUtils.writePlayerKey(buf, this.identifiedKey);
  }
}
