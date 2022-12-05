package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ChatSessionUpdate implements MinecraftPacket {
  private @Nullable RemoteChatSession session;

  public ChatSessionUpdate() {
    this(null);
  }

  public ChatSessionUpdate(@Nullable RemoteChatSession session) {
    this.session = session;
  }

  public @Nullable RemoteChatSession getSession() {
    return session;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.session = new RemoteChatSession(protocolVersion, buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (this.session != null) {
      this.session.write(buf);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return false;
  }
}
