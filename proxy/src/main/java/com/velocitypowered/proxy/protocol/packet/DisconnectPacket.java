package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DisconnectPacket implements Packet {

  private @Nullable String reason;

  public DisconnectPacket() {
  }

  public DisconnectPacket(String reason) {
    this.reason = Preconditions.checkNotNull(reason, "reason");
  }

  public String getReason() {
    if (reason == null) {
      throw new IllegalStateException("No reason specified");
    }
    return reason;
  }

  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  @Override
  public String toString() {
    return "Disconnect{"
        + "reason='" + reason + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    reason = ProtocolUtils.readString(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (reason == null) {
      throw new IllegalStateException("No reason specified.");
    }
    ProtocolUtils.writeString(buf, reason);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static DisconnectPacket create(Component component, ProtocolVersion version) {
    Preconditions.checkNotNull(component, "component");
    return new DisconnectPacket(ProtocolUtils.getJsonChatSerializer(version).serialize(component));
  }
}
