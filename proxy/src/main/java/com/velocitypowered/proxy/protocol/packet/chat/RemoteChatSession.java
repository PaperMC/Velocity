package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RemoteChatSession {
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

  public void validate() {

  }
}
